# Multi-stage build: compile with Maven, run with JRE.
# Build: docker build --build-arg SERVICE_MODULE=auth-service -t ticketing/auth-service:latest .
ARG SERVICE_MODULE
FROM eclipse-temurin:17-jdk-alpine AS builder
ARG SERVICE_MODULE
WORKDIR /build

# Copy parent POM and all module POMs
COPY pom.xml .
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY booking-service/pom.xml booking-service/
COPY movie-service/pom.xml movie-service/
COPY payment-service/pom.xml payment-service/
COPY theatre-service/pom.xml theatre-service/

# Download dependencies (cache layer)
RUN mvn -B dependency:go-offline -pl ${SERVICE_MODULE} -am || true

# Copy source for the service and its dependencies
COPY auth-service/src auth-service/src
COPY user-service/src user-service/src
COPY booking-service/src booking-service/src
COPY movie-service/src movie-service/src
COPY payment-service/src payment-service/src
COPY theatre-service/src theatre-service/src

# Build the service (skip tests for smaller image; run tests in CI before build)
RUN mvn -B package -pl ${SERVICE_MODULE} -am -DskipTests -q && \
    cp ${SERVICE_MODULE}/target/*.jar app.jar

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=builder /build/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
