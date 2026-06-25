# ecommerce-microservices

A microservices-based e-commerce application built with Spring Boot, demonstrating
REST inter-service communication and resilience patterns (Resilience4j).

## Services

| Service              | Port | Responsibility                                  |
|----------------------|------|-------------------------------------------------|
| auth-server          | 9000 | OAuth2 authorization server (issues JWTs)       |
| api-gateway          | 8080 | Single entry point; validates JWTs, routes      |
| product-service      | 8081 | Product catalog and stock management            |
| order-service        | 8082 | Order placement; reserves stock from product    |
| notification-service | 8083 | Sends notifications when orders are placed       |

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
├── product-service/         catalog + stock
├── order-service/           orders + Resilience4j-protected call to product
└── notification-service/    notifications
```

The `common` module deliberately contains only DTOs/contracts, no business logic,
to avoid re-coupling the services.

## Persistence

Currently uses an in-memory H2 database per service so the system runs with no
external setup. Each H2 console is available at `/h2-console` on the service port.
Swapping to PostgreSQL/RDS later requires only `application.yml` and dependency
changes — no code changes, since persistence goes through Spring Data JPA.

## Inter-service communication

- order-service calls product-service over REST to reserve stock before an order
  is committed (synchronous validation hop).
- The call is wrapped with Resilience4j **CircuitBreaker** + **Retry**. Idempotency
  keys on the reserve request make retries safe over an unreliable network.

## Tests

Run all tests with `mvn test` (or `mvn verify`). Two layers:

Unit tests (JUnit 5 + Mockito, no Spring context, no DB) cover the service logic:
`ProductServiceTest` (idempotent reservation, stock decrement, insufficient-stock and
not-found cases), `OrderServiceTest` (reservation orchestration: confirm-and-notify on
success, reject on insufficient stock), and `NotificationServiceTest`.

Integration tests (`@SpringBootTest` + MockMvc against in-memory H2, activated by the `test`
profile) cover controller -> service -> JPA: `ProductControllerIT`, `OrderControllerIT`,
`NotificationControllerIT`. They assert real HTTP status codes and JSON, including that
endpoints return 401 without a token and succeed with one. Security is satisfied with a mock
JWT (`spring-security-test`), and a stub `JwtDecoder` avoids contacting the auth-server at
startup. In `OrderControllerIT` the downstream clients are mocked, so the test runs without
product-service or notification-service.

The integration tests use H2 (zero setup). For higher fidelity against real Postgres,
Testcontainers would spin up a Postgres container per run -- not used here to avoid a Docker
dependency.

## Running locally (IntelliJ)

Each service has its own `@SpringBootApplication` main class. Create a separate run
configuration per service and start them independently; all three can run at once on
their respective ports.

Or from the command line:

```bash
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl notification-service spring-boot:run
```

Build everything:

```bash
mvn clean install
```

## API endpoints

### product-service (8081)
| Method | Path                | Description                          |
|--------|---------------------|--------------------------------------|
| GET    | /products           | List all products                    |
| GET    | /products/{id}      | Get a product by id                  |
| POST   | /products           | Create a product                     |
| PUT    | /products/{id}      | Update a product                     |
| POST   | /products/reserve   | Reserve stock (Idempotency-Key header) |

### order-service (8082)
| Method | Path           | Description                              |
|--------|----------------|------------------------------------------|
| GET    | /orders        | List all orders                          |
| GET    | /orders/{id}   | Get an order by id                       |
| POST   | /orders        | Place an order (reserves stock + notifies) |

### notification-service (8083)
| Method | Path            | Description           |
|--------|-----------------|-----------------------|
| POST   | /notifications  | Send a notification   |
| GET    | /notifications  | List sent notifications |

Sample requests for all endpoints are in `requests.http` (runnable directly in IntelliJ).

## Swagger / OpenAPI

Each service serves its own Swagger UI (springdoc-openapi), generated automatically from the
controllers and DTOs:

| Service              | Swagger UI                              | OpenAPI JSON                       |
|----------------------|-----------------------------------------|------------------------------------|
| product-service      | http://localhost:8081/swagger-ui.html   | http://localhost:8081/v3/api-docs  |
| order-service        | http://localhost:8082/swagger-ui.html   | http://localhost:8082/v3/api-docs  |
| notification-service | http://localhost:8083/swagger-ui.html   | http://localhost:8083/v3/api-docs  |

The doc pages are whitelisted in Spring Security, so they open without a token. The API calls
themselves still require one: click "Authorize" in the UI, paste an access token (see
SECURITY.md for how to get one), and try-it-out calls will carry the bearer token.

A single aggregated Swagger behind the gateway is possible but not configured here; per-service
UIs document all endpoints.

## Resilience: how it behaves

The order -> product reserve call is wrapped with Resilience4j **Retry** + **CircuitBreaker**
(instance name `productService`, configured in order-service `application.yml`).

- **Insufficient stock / unknown product**: product-service returns 409 / 404. The order is
  rejected with **422**. These are valid business answers and do NOT count as circuit-breaker
  failures.
- **product-service down or hung**: the call fails, Retry attempts it again (safe because of
  the idempotency key), and if failures persist the circuit **opens** and order placement fails
  fast with **503** via the client fallback.

To see the breaker trip: start order-service but NOT product-service, then POST a few orders.
Watch `GET /actuator/circuitbreakers` move the `productService` breaker from CLOSED to OPEN.

## Known limitations (intentional, scoped for the assignment)

- **No compensation across order lines.** If an order has several lines and a later line fails
  to reserve, stock reserved for earlier lines is not released. A full saga would emit a
  compensating release. This is the documented trade-off of the synchronous-reserve design.
- **Per-call idempotency only.** order-service generates a fresh idempotency key per reserve
  call, which protects the network hop. End-to-end "place this exact order once" would need a
  client-supplied order-level key.
- **Notification is best-effort over REST.** A notification failure is logged and swallowed so
  it never fails a committed order. In production this hop would move to an event/queue.
