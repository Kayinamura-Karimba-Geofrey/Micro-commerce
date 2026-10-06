# Micro-commerce

A production-style Microservices E-commerce Platform built with Spring Boot and PostgreSQL.

## Architecture
- **User Service**: Authentication & Registration (Spring Security + JWT)
- **Product Service**: Inventory & Category Management (Redis-cached)
- **Order Service**: Order processing & Stock verification (OpenFeign), publishes `order-placed` Kafka events
- **Payment Service**: Mock payment processing, consumes `order-placed` Kafka events
- **Discovery Server**: Service Discovery (Netflix Eureka)
- **Config Server**: Central configuration (native profile, served from `config-repo/`)
- **API Gateway**: Single entry point (Spring Cloud Gateway) with JWT validation
- **Database**: PostgreSQL (separate databases per service)
- **Orchestration**: Docker & Docker Compose

## Security model
- The API gateway validates JWTs on every secured route.
- Internal services are **not** published on host ports; all traffic must go through
  the gateway (`http://localhost:8080`), so requests cannot bypass authentication.
- The gateway's Eureka discovery locator is disabled because its auto-generated
  `/{service-id}/**` routes bypass route filters.
- The gateway forwards the verified identity to the services as `X-User-Id`,
  `X-User-Email` and `X-User-Role` headers (any client-supplied values are
  overwritten). Order and payment services use them for ownership checks: users
  can only read their own orders and payments.
- Reading products requires a valid token; creating or deleting products
  requires the `ADMIN` role. New users always register as `CUSTOMER`; promote an
  admin directly in the database (`UPDATE users SET role='ADMIN' WHERE email=...`)
  and log in again to get a token with the new role.
- JWT secret, database and Grafana passwords have **no defaults**: docker compose
  refuses to start until they are set in `.env` (see `.env.example`).
- Infrastructure ports (Postgres, Kafka, Redis, Eureka, Config Server, Zipkin,
  Prometheus, Grafana) are bound to `127.0.0.1` only. Only the gateway (8080) is
  exposed on all interfaces. Gateway metrics are served on port 8090, which is
  not published.
- Containers run as a non-root user.

## Prerequisites
- Java 21+
- Maven
- Docker & Docker Compose

## How to Run
1. Clone the repository.
2. Build the service jars: `mvn clean package -DskipTests`
3. Copy `.env.example` to `.env` and set `JWT_SECRET` (`openssl rand -base64 48`),
   `POSTGRES_PASSWORD` and `GRAFANA_ADMIN_PASSWORD`.
4. Start everything: `docker compose up -d --build`
5. Access the services through the API Gateway at `http://localhost:8080`.

On first startup the PostgreSQL container creates `user_db`, `product_db`,
`order_db` and `payment_db` automatically (see `docker/init.sql`). If you have an
older `postgres_data` volume, reset it with `docker compose down -v` first.

## Ports
All ports except the API Gateway are reachable from `localhost` only.

| Service          | Port |
|------------------|------|
| API Gateway      | 8080 |
| Discovery Server | 8761 |
| Config Server    | 8888 |
| PostgreSQL       | 5432 |
| Kafka (host listener) | 9092 |
| Redis            | 6380 (host) |
| Zipkin           | 9411 |
| Prometheus       | 9090 |
| Grafana          | 3000 |

Note: Kafka runs a single broker with two listeners. Containers connect to
`kafka:29092` (internal), tools on your machine connect to `localhost:9092`.

## API Endpoints
All requests go through the gateway at `http://localhost:8080`:

- **Auth**: `POST /api/v1/auth/register`, `POST /api/v1/auth/authenticate`
- **Products**: `GET /api/v1/products`, `GET /api/v1/products/{id}`, `POST /api/v1/products` (admin), `DELETE /api/v1/products/{id}` (admin)
- **Orders**: `POST /api/v1/orders`, `GET /api/v1/orders/{id}`
- **Payments**: `POST /api/v1/payments` with body `{"orderId": 1}` (settles the pending payment of your order), `GET /api/v1/payments/order/{id}`
