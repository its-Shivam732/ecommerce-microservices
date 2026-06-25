# ecommerce-microservices

A microservices-based e-commerce application built with Spring Boot, demonstrating REST
inter-service communication, resilience patterns (Resilience4j), event-driven messaging
(Kafka), OAuth2 security, and PostgreSQL persistence on AWS RDS.

## Services

| Service              | Port | Responsibility                                  |
|----------------------|------|-------------------------------------------------|
| auth-server          | 9000 | OAuth2 authorization server (issues JWTs)       |
| api-gateway          | 8080 | Single entry point; validates JWTs, routes      |
| product-service      | 8081 | Product catalog and stock management            |
| order-service        | 8082 | Order placement; reserves stock, publishes events |
| notification-service | 8083 | Sends notifications (REST endpoint + Kafka listener) |

Each business service is an independent Spring Boot application with its own database.
They are organized as Maven modules in a single repository (monorepo) for ease of
development; at scale, each would typically live in its own repository.

Security is documented separately in `SECURITY.md`. In short: all client traffic goes
through the gateway on port 8080 with a `Bearer` JWT obtained from the auth-server.

## Module layout

```
ecommerce-microservices/
├── pom.xml                  parent (packaging=pom), shared dependency management
├── common/                  shared cross-service DTOs and event payloads (wire contracts only)
├── auth-server/             OAuth2 authorization server (Spring Authorization Server)
├── api-gateway/             Spring Cloud Gateway; validates JWTs and routes
├── product-service/         catalog + stock
├── order-service/           orders + Resilience4j-protected call to product + Kafka producer
└── notification-service/    notifications (REST + Kafka consumer)
```

The `common` module deliberately contains only DTOs/contracts and event payloads, no
business logic, to avoid re-coupling the services.

## Persistence

Each service uses its own PostgreSQL database on a shared AWS RDS instance
(`productdb`, `orderdb`, `notificationdb`) — one logical database per service, with no
cross-service queries. This preserves the database-per-service boundary while fitting the
RDS free tier (one instance); in production each would be promoted to its own instance for
true physical isolation and independent scaling.

Connection details live in each service's `application.yml`. The database password is
supplied via the `DB_PASSWORD` environment variable rather than committed to source.
Persistence goes through Spring Data JPA, so the database choice is isolated to
configuration. The H2 driver is also retained in each POM for offline/local runs and is
used by the integration tests.

## Reservation and idempotency (how the synchronous transaction is kept safe)

This is the core of the cross-service write path and the most important design detail.
Placing an order requires order-service and product-service to *both* change state — an
order row is inserted in `orderdb` and stock is decremented in `productdb`. These are two
separate databases reached over the network, so there is no single ACID transaction
spanning them; a naive "decrement then insert" is a dual-write that can leave the system
inconsistent if the network fails between the two steps (e.g. product decrements stock but
the HTTP response is lost, so order-service never learns it succeeded).

The design addresses this with **idempotent stock reservation**:

1. For each order line, order-service generates a unique **idempotency key** (a UUID) and
   sends it with the reserve request to product-service (`POST /products/reserve`, key in
   the `Idempotency-Key` header).
2. product-service, before decrementing, checks whether it has already processed that key
   (a `processed_reservations` table with a unique constraint on the key). If yes, it
   returns the **stored outcome** without decrementing again. If no, it decrements stock,
   records the key + outcome, and returns.
3. Crucially, the key insert and the stock decrement happen in **one local transaction
   inside product-service**. Because that is a single database, it is a normal ACID
   transaction — the decrement and the "processed" record commit together or not at all.

This makes the reserve call **safe to retry**. When Resilience4j's Retry re-sends a call
after a transient failure (a dropped connection, a lost response), the same idempotency key
guarantees the stock is not decremented twice — the retry either completes the original
operation or returns the outcome that already happened. Idempotency is precisely what turns
"did that call go through?" from an unanswerable question into a safe re-attempt.

Concurrency is handled two ways: the unique constraint on the idempotency key rejects a
duplicate concurrent request at the database level, and an `@Version` optimistic-lock column
on the product row prevents two simultaneous reservations from both succeeding against a
stale stock count (the second commit fails rather than overselling).

Trade-off and honest boundary: this protects the *network hop* and prevents double
decrements, but it is not a full distributed transaction. If order-service ultimately gives
up after exhausting retries while the decrement *did* succeed, the reserved stock is
orphaned. A production system would add a reserve-then-confirm flow with a timeout (stock is
held, then released by a reaper if no confirm arrives) or a reconciliation job — and for a
multi-step workflow, a saga with compensating transactions. Those are the documented next
steps; the idempotent reserve is the scoped, correct core for this assignment.

## Inter-service communication

Synchronous (REST + Resilience4j): order-service calls product-service to reserve stock
before an order is committed. The call is wrapped with Resilience4j **CircuitBreaker** +
**Retry**, and an idempotency key on the reserve request makes retries safe over an
unreliable network. Outbound calls carry a client-credentials JWT obtained from the
auth-server, since the downstream services are protected.

Asynchronous (Kafka): when an order is confirmed, order-service also publishes an
`OrderPlaced` event to the `order-notifications` Kafka topic. notification-service consumes
it and creates a notification.
**This is in addition to the REST notification path (POST endpoint given in assignment), demonstrating the event-driven pattern alongside the synchronous one. This is just for homework sake where we demonstrate both aynchronus(kafka) and synchronus(rest) message passing**
## Messaging (Kafka)

Kafka runs locally in Docker for development:

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:3.8.0
```

Create the topic:

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create --topic order-notifications \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1
```

On placing an order, notification-service receives the notification two ways: via the
synchronous REST call (channel `EMAIL`) and via the Kafka event (channel `EMAIL_KAFKA`).
The Kafka-originated row is tagged so the two paths are distinguishable. In production the
notification hop would move entirely onto the event/queue, with the REST endpoint retained
only for direct/manual sends. (Locally Kafka runs in Docker; in production this would be a
managed broker such as AWS MSK or Confluent Cloud.)

## Security (OAuth2)

The auth-server (Spring Authorization Server) issues JWT access tokens using the
client-credentials grant. The gateway and all three services validate tokens. See
`SECURITY.md` for the token-fetch curl and full details.

## Tests

Run all tests with `mvn test`. Set `JAVA_HOME` to a JDK 17 first if running from the
terminal (`export JAVA_HOME=$(/usr/libexec/java_home -v 17)`). Two layers:

Unit tests (JUnit 5 + Mockito, no Spring context, no DB) cover the service logic:
`ProductServiceTest` (idempotent reservation, stock decrement, insufficient-stock and
not-found cases), `OrderServiceTest` (reservation orchestration: confirm-and-notify on
success, reject on insufficient stock), and `NotificationServiceTest`.

Integration tests (`@SpringBootTest` + MockMvc against in-memory H2, activated by the
`test` profile) cover controller -> service -> JPA: `ProductControllerIT`,
`OrderControllerIT`, `NotificationControllerIT`. They assert real HTTP status codes and
JSON, including that endpoints return 401 without a token and succeed with one. Security is
satisfied with a mock JWT (`spring-security-test`), and a stub `JwtDecoder` avoids
contacting the auth-server at startup. In `OrderControllerIT` the downstream clients and the
Kafka publisher are mocked, so the test runs without product-service, notification-service,
or a running Kafka broker.

The integration tests use H2 (zero setup). For higher fidelity against real Postgres,
Testcontainers would spin up a Postgres container per run -- not used here to avoid a Docker
dependency for the test suite.

## Running locally

Start order: **Kafka (Docker)** -> **auth-server (9000)** -> **product / notification /
order (8081 / 8083 / 8082)** -> **api-gateway (8080)**.

Environment variables (per IntelliJ run config, or exported in the shell):

```
DB_PASSWORD=<your-rds-password>          # all three business services
OAUTH_CLIENT_SECRET=ecommerce-secret     # order-service (defaults to this if unset)
```

Each service has its own `@SpringBootApplication` main class; create a run configuration
per service and start them independently. Or from the command line:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn -pl auth-server spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Build everything:

```bash
mvn clean install
```

## API endpoints

All endpoints require a `Bearer` JWT. Through the gateway, use `http://localhost:8080`;
directly, use each service's own port.

### product-service (8081)
| Method | Path                | Description                          |
|--------|---------------------|--------------------------------------|
| GET    | /products           | List all products                    |
| GET    | /products/{id}      | Get a product by id                  |
| POST   | /products           | Create a product                     |
| PUT    | /products/{id}      | Update a product                     |
| POST   | /products/reserve   | Reserve stock (Idempotency-Key header) |

### order-service (8082)
| Method | Path           | Description                                |
|--------|----------------|--------------------------------------------|
| GET    | /orders        | List all orders                            |
| GET    | /orders/{id}   | Get an order by id                         |
| POST   | /orders        | Place an order (reserves stock, notifies, publishes Kafka event) |

### notification-service (8083)
| Method | Path            | Description                                  |
|--------|-----------------|----------------------------------------------|
| POST   | /notifications  | Send a notification                          |
| GET    | /notifications  | List sent notifications (REST + Kafka rows)  |

Sample requests are in `requests.http` (runnable directly in IntelliJ).

## Swagger / OpenAPI

Each service serves its own Swagger UI (springdoc-openapi), generated from the controllers
and DTOs:

| Service              | Swagger UI                              | OpenAPI JSON                       |
|----------------------|-----------------------------------------|------------------------------------|
| product-service      | http://localhost:8081/swagger-ui.html   | http://localhost:8081/v3/api-docs  |
| order-service        | http://localhost:8082/swagger-ui.html   | http://localhost:8082/v3/api-docs  |
| notification-service | http://localhost:8083/swagger-ui.html   | http://localhost:8083/v3/api-docs  |

Doc pages are whitelisted in Spring Security, so they open without a token. The API calls
themselves require one: click "Authorize", paste an access token (see `SECURITY.md`), and
try-it-out calls carry the bearer token.

## Resilience: how it behaves

The order -> product reserve call is wrapped with Resilience4j **Retry** + **CircuitBreaker**
(instance `productService`, configured in order-service `application.yml`).

- **Insufficient stock / unknown product**: product-service returns 409 / 404; the order is
  rejected with **422**. These are valid business answers and do NOT count as circuit-breaker
  failures.
- **product-service down or hung**: the call fails, Retry attempts it again (safe because of
  the idempotency key), and if failures persist the circuit **opens** and order placement
  fails fast with **503** via the client fallback.

To see the breaker trip: start order-service but NOT product-service, POST a few orders, and
watch `GET http://localhost:8082/actuator/circuitbreakers` move the breaker CLOSED -> OPEN.

## Known limitations (intentional, scoped for the assignment)

- **No compensation across order lines.** If an order has several lines and a later line
  fails to reserve, stock reserved for earlier lines is not released. A full saga would emit
  a compensating release. This is the documented trade-off of the synchronous-reserve design.
- **Per-call idempotency only.** order-service generates a fresh idempotency key per reserve
  call, which protects the network hop. End-to-end "place this exact order once" would need a
  client-supplied order-level key.
- **Dual notification paths.** Both the REST call and the Kafka event create a notification
  for a placed order, so a confirmed order produces two notification rows (channels `EMAIL`
  and `EMAIL_KAFKA`). This is intentional for the demo, to make the event-driven path visible;
  a production system would use one path (the event) and retain REST only for manual sends.
- **Auth-server signing key is regenerated at each startup**, so restarting the auth-server
  invalidates previously issued tokens. A fixed key from config would persist them.
