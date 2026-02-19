# Movie Ticket Booking Platform

A microservices-based platform for browsing movies and shows, reserving seats, and completing payments. Partners (theatres) can manage theatres, screens, and show timings.

## Tech Stack

- **Java 17**, **Spring Boot 3.5.x**
- **PostgreSQL** (one database per service)
- **REST APIs** (OpenAPI 3.0, code-generated)
- **JWT** for authentication (access + refresh tokens)

## Services

| Service          | Port (local) | Role                                          |
|------------------|-------------|-----------------------------------------------|
| user-service     | 8080        | Registration, login (B2C & B2B); calls auth for tokens |
| auth-service     | 8081        | Issue/refresh JWT; stores refresh tokens      |
| theatre-service  | 8082        | Theatres, screens, seats; lock/release/confirm |
| movie-service    | 8083        | Movies, shows; browse & partner CRUD          |
| payment-service  | 8084        | Payment records (create, confirm)            |
| booking-service  | 8085        | Bookings; orchestrates reserve & confirm     |

Booking-service calls theatre-service (seat lock) and payment-service; user-service calls auth-service for tokens.

## Prerequisites

- **JDK 17**
- **Maven 3.8+**
- **PostgreSQL** (each service expects its own DB; configure via `application.properties` or env)

## Build & Run

```bash
# Build all modules
mvn clean install

# Run a service (example: auth-service)
cd auth-service && mvn spring-boot:run
```

Run each service in a separate terminal (or configure your IDE). Ensure PostgreSQL is up and datasource URL/credentials match each service’s `application.properties`. Service URLs (e.g. `theatre.service.base-url`, `payment.service.base-url`, `auth.service.base-url`) are set for localhost in config.

## Docker & Kubernetes

- **Docker:** Multi-stage build; use `SERVICE_MODULE` to build one service, e.g.  
  `docker build --build-arg SERVICE_MODULE=auth-service -t ticketing/auth-service:latest .`
- **Kubernetes:** Manifests under `k8s/` (namespace, deployments, services, ConfigMap, secrets). See `docs/` for deployment notes and the design document.

## Documentation

- **Design & architecture:** Movie_Ticket_Booking_Platform_Design_And_Architecture.docx` 


