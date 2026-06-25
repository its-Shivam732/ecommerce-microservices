# Security (OAuth2)

The API is secured with OAuth2 using Spring Authorization Server (token issuer) and
Spring Security resource servers (token validators). No Docker, no external IdP — the
authorization server is a module in this repo.

## Components

| Module        | Port | Role                                                        |
|---------------|------|-------------------------------------------------------------|
| auth-server   | 9000 | Authorization server. Issues JWT access tokens.             |
| api-gateway   | 8080 | Front door. Validates JWTs, routes to the three services.   |
| product/order/notification | 8081/8082/8083 | Resource servers. Each validates JWTs too. |

## Grant type: client credentials

The consumers here are API clients and services (not interactive users), so we use the
**client-credentials** grant — exchange a client id + secret for a token, no login page.
The registered client:

- client id: `ecommerce-client`
- client secret: `ecommerce-secret`
- scopes: `api.read`, `api.write`

(For a user-facing app you'd use authorization-code + PKCE instead; client-credentials is the
correct grant for machine-to-machine access.)

## How tokens flow

1. A client (Postman/curl, or order-service for its internal calls) requests a token from the
   auth-server's `/oauth2/token` endpoint using the client credentials.
2. The auth-server returns a signed JWT.
3. The client sends the token as `Authorization: Bearer <token>` to the gateway (port 8080).
4. The gateway validates the token against the auth-server's public keys (JWKS) and routes the
   request to the right service.
5. Each service also validates the token (defense in depth) before processing.

order-service additionally obtains its own client-credentials token to call product-service
and notification-service, since those are now protected.

## Start order

1. `auth-server` (9000)
2. `product-service`, `notification-service`, `order-service` (8081/8082/8083)
3. `api-gateway` (8080)

## Environment variables

Set these (per run config in IntelliJ, or in your shell):

```
DB_PASSWORD=<your-rds-password>
OAUTH_CLIENT_SECRET=ecommerce-secret   # order-service only; defaults to this if unset
```

## Getting a token (curl)

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u ecommerce-client:ecommerce-secret \
  -d grant_type=client_credentials \
  -d scope="api.read api.write"
```

Response contains `access_token`. Copy it.

## Calling the API through the gateway

```bash
TOKEN="<paste access_token>"

# Without a token -> 401
curl -i http://localhost:8080/products

# With a token -> 200 and the product list
curl -i http://localhost:8080/products -H "Authorization: Bearer $TOKEN"
```

The 401-vs-200 difference is the demonstrable proof that security is working.

`requests.http` has these requests ready to run in IntelliJ — grab a token with the first
request, paste it into the `@token` variable at the top, then run the rest.

## Notes / limitations (scoped for the assignment)

- The signing key is generated fresh at each auth-server startup. Restarting the auth-server
  invalidates previously issued tokens. For persistence you'd load a fixed key from config.
- The client secret is stored as plain text (`{noop}`) for simplicity. Production would hash it
  and source it from a secrets manager.
- order-service reuses the same `ecommerce-client` credentials for its internal calls. A
  stricter setup would give each service its own client identity and scopes.
