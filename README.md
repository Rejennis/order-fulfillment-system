# Order Fulfillment & Notification System

[![CI Pipeline](https://github.com/Rejennis/order-fulfillment-system/workflows/CI%20Pipeline/badge.svg)](https://github.com/Rejennis/order-fulfillment-system/actions)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> A production-ready backend system demonstrating mid-level Java engineering through practical implementation of Domain-Driven Design, Event-Driven Architecture, and modern DevOps practices.

**Built as part of the 14-day "Be Prolific - Gulp Life" Mid-Level Java Developer Mentor Program**

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Observability](#observability)
- [Project Journey](#project-journey)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

This project is a comprehensive **Order Fulfillment and Notification System** that manages the complete lifecycle of customer orders from creation through payment, shipping, and delivery. Built with production-grade practices, it demonstrates:

- **Domain-Driven Design (DDD)** with clear bounded contexts
- **Hexagonal Architecture** for maintainability and testability
- **Event-Driven Architecture** for loose coupling and scalability
- **RESTful API Design** following HTTP semantics
- **Comprehensive Testing** (Unit, Integration, E2E)
- **Observability** with metrics, logging, and health checks
- **DevOps Automation** with Docker and CI/CD pipelines

### Business Domain

The system manages orders through the following state machine:

```
CREATED → PAID → SHIPPED → DELIVERED
   ↓
CANCELLED (possible from any state before SHIPPED)
```

**Core Capabilities:**
- ✅ Create orders with multiple line items and shipping addresses
- ✅ Process payments with idempotency guarantees
- ✅ Track shipment and delivery status
- ✅ Send real-time notifications at each lifecycle event (via Kafka)
- ✅ Handle errors gracefully with retry and circuit breaker patterns
- ✅ Provide comprehensive observability (metrics, logs, traces)
- ✅ Secure with JWT authentication and role-based authorization

## ✨ Key Features

### Domain Model (DDD)
- **Order Aggregate** with enforced state machine transitions
- **Value Objects** (Money, OrderItem, Address) for type safety
- **Business Rule Validation** at the domain level (not in controllers)
- **Immutability** and defensive copying for thread safety
- **Domain Events** (OrderCreated, OrderPaid, OrderShipped, etc.)

### REST API
- RESTful endpoints following HTTP semantics
- Proper status codes (200, 201, 400, 401, 403, 404, 409, 500)
- Request/response DTOs separate from domain models
- Bean Validation for input sanitization
- Global exception handling with meaningful error messages
- OpenAPI/Swagger documentation

### Event-Driven Architecture
- **Domain Events** published on aggregate state changes
- **Kafka Integration** for reliable, scalable event streaming
- **Event Listeners** for notifications and auditing
- **Dual Publishing Strategy**: Transactional (Spring Events) + Durable (Kafka)
- **Idempotent Consumers** to handle duplicate events safely
- **Retry Logic** with exponential backoff

### Persistence
- **PostgreSQL** for reliable transactional data storage
- **JPA/Hibernate** with optimized queries (no N+1 issues)
- **Testcontainers** for integration testing with real database
- **Flyway** migrations ready for schema versioning
- **Transaction Management** with Spring @Transactional
- **Auditing** with @CreatedDate and @LastModifiedDate

### Notifications
- **Port/Adapter Pattern** for pluggable notification providers
- **Async Processing** with @Async and dedicated thread pools
- **Email Notifications** (mock implementation, production-ready interface)
- **Event-Driven Triggers** from order lifecycle changes
- **Kafka-Based Notifications** for reliability and scalability

### Observability
- **Structured Logging** (JSON format with MDC correlation IDs)
- **Metrics** with Micrometer (Prometheus format)
- **Health Checks** for all dependencies (database, Kafka)
- **Request Tracing** for debugging production issues
- **Actuator Endpoints** (/health, /metrics, /prometheus, /info)
- **Custom Metrics** for business events (order counts, failures)

### Resilience
- **Global Exception Handling** with @RestControllerAdvice
- **Retry Logic** with Spring Retry and @Retryable
- **Circuit Breaker** with Resilience4j for external dependencies
- **Transaction Boundaries** to ensure ACID properties
- **Graceful Degradation** when services are unavailable
- **Idempotency** for payment operations

### Security
- **JWT Authentication** with token-based auth
- **Role-Based Authorization** (ADMIN, USER roles) with @PreAuthorize
- **BCrypt Password Hashing** for user credentials
- **User Entity** integrated with Spring Security
- **Secured Endpoints** with method-level security
- **CORS Configuration** for frontend integration

### DevOps
- **Multi-Stage Dockerfile** (~250MB optimized image, Alpine-based)
- **Docker Compose** for full stack local deployment (6 services)
- **GitHub Actions CI/CD** with 4-job pipeline (build, quality, docker, summary)
- **Code Quality Checks** (SpotBugs for bugs, Checkstyle for style)
- **Security Scanning** with Trivy for vulnerabilities
- **Automated Testing** in CI with service containers (PostgreSQL, Kafka)
- **Layer Caching** for fast Docker builds

## 🏗️ Architecture

This system follows **Hexagonal Architecture** (Ports and Adapters) to maintain clean separation of concerns:

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ADAPTERS (IN)                            │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐             │
│  │   REST     │  │  GraphQL   │  │   CLI      │             │
│  │ Controller │  │  (Future)  │  │  (Future)  │             │
│  └─────┬──────┘  └──────┬─────┘  └──────┬─────┘             │
└────────┼─────────────────┼───────────────┼──────────────────┘
         │                 │               │
         └─────────────────┴───────────────┘
                           │
         ┌─────────────────▼────────────────┐
         │      APPLICATION LAYER           │
         │  ┌────────────────────────────┐  │
         │  │     OrderService           │  │
         │  │  - Business Orchestration  │  │
         │  │  - Transaction Management  │  │
         │  │  - Event Publishing        │  │
         │  └────────────────────────────┘  │
         └──────────────┬───────────────────┘
                        │
         ┌──────────────▼───────────────────┐
         │         DOMAIN LAYER             │
         │  ┌─────────────────────────────┐ │
         │  │  Order (Aggregate Root)     │ │
         │  │  - Business Rules           │ │
         │  │  - State Machine            │ │
         │  │  - Domain Events            │ │
         │  │  - Validation Logic         │ │
         │  └─────────────────────────────┘ │
         │                                  │
         │  ┌─────────────────────────────┐ │
         │  │  Value Objects              │ │
         │  │  - Money (BigDecimal)       │ │
         │  │  - OrderItem                │ │
         │  │  - Address                  │ │
         │  └─────────────────────────────┘ │
         │                                  │
         │  ┌─────────────────────────────┐ │
         │  │  Ports (Interfaces)         │ │
         │  │  - OrderRepository          │ │
         │  │  - NotificationPort         │ │
         │  │  - UserRepository           │ │
         │  └─────────────────────────────┘ │
         └──────────────┬───────────────────┘
                        │
┌───────────────────────▼────────────────────────────────────┐
│                    ADAPTERS (OUT)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ PostgreSQL   │  │    Kafka     │  │    Email     │      │
│  │  (JPA)       │  │  Publisher   │  │  Notifier    │      │
│  │  Adapter     │  │  Adapter     │  │  Adapter     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────────────────────────────────────────┘
```

### Data Flow for Order Creation

```mermaid
sequenceDiagram
    participant Client
    participant REST as REST Controller
    participant Service as OrderService
    participant Domain as Order (Aggregate)
    participant DB as PostgreSQL
    participant Kafka as Kafka Broker
    participant Listener as Event Listener

    Client->>REST: POST /api/orders
    REST->>Service: createOrder(request)
    Service->>Domain: Order.create()
    Domain-->>Service: Order (CREATED status)
    Service->>DB: save(order)
    DB-->>Service: saved order
    Service->>Service: publishEvent(OrderCreatedEvent)
    Service->>Kafka: send(OrderCreatedEvent)
    Service-->>REST: OrderResponse
    REST-->>Client: 201 Created
    
    Kafka->>Listener: OrderCreatedEvent
    Listener->>Listener: sendNotification()
```

### Package Structure

```
com.midlevel.orderfulfillment/
├── domain/                    # Domain Layer (pure business logic)
│   ├── model/
│   │   ├── Order.java        # Aggregate Root with state machine
│   │   ├── OrderStatus.java  # Enum for order states
│   │   ├── OrderItem.java    # Value Object for line items
│   │   ├── Money.java        # Value Object for currency
│   │   ├── Address.java      # Value Object for addresses
│   │   └── User.java         # User entity for security
│   ├── event/
│   │   ├── OrderCreatedEvent.java
│   │   ├── OrderPaidEvent.java
│   │   ├── OrderShippedEvent.java
│   │   └── OrderDeliveredEvent.java
│   └── port/                 # Interfaces (dependency inversion)
│       ├── OrderRepository.java
│       ├── NotificationPort.java
│       └── UserRepository.java
│
├── application/               # Application Layer (orchestration)
│   └── service/
│       ├── OrderService.java         # Main business orchestration
│       ├── AuthService.java          # Authentication logic
│       └── NotificationService.java  # Notification coordination
│
├── adapter/                   # Adapters Layer (external world)
│   ├── in/                   # Inbound Adapters
│   │   └── web/
│   │       ├── OrderController.java  # REST endpoints
│   │       ├── AuthController.java   # Auth endpoints
│   │       └── dto/                  # Request/Response DTOs
│   │           ├── CreateOrderRequest.java
│   │           ├── OrderResponse.java
│   │           └── ErrorResponse.java
│   │
│   └── out/                  # Outbound Adapters
│       ├── persistence/
│       │   ├── JpaOrderRepository.java      # JPA implementation
│       │   ├── JpaUserRepository.java
│       │   └── entity/                      # JPA entities
│       │       ├── OrderEntity.java
│       │       └── UserEntity.java
│       │
│       ├── messaging/
│       │   ├── KafkaEventPublisher.java     # Kafka producer
│       │   └── KafkaEventListener.java      # Kafka consumer
│       │
│       └── notification/
│           └── EmailNotificationAdapter.java # Email sender
│
└── config/                    # Configuration
    ├── KafkaConfig.java
    ├── SecurityConfig.java
    ├── AsyncConfig.java
    ├── JpaAuditingConfig.java
    └── WebConfig.java
```

### Key Architectural Decisions

Full Architecture Decision Records (ADRs) are available in [docs/architecture/](docs/architecture/):

1. **[ADR-001: Hexagonal Architecture](docs/architecture/adr-001-hexagonal-architecture.md)** - Clean separation, testability, maintainability
2. **[ADR-002: Event-Driven Notifications](docs/architecture/adr-002-event-driven-notifications.md)** - Loose coupling, async processing, scalability
3. **[ADR-003: JPA for Persistence](docs/architecture/adr-003-jpa-for-persistence.md)** - ORM benefits, Spring Data integration, developer productivity

## 🛠️ Technology Stack

### Core Framework
- **Java 17** - Modern LTS with records, pattern matching, text blocks
- **Spring Boot 3.2.1** - Dependency injection, auto-configuration
- **Spring Data JPA** - Repository abstraction, query methods
- **Spring Web** - REST controllers, exception handling
- **Spring Security 6** - Authentication and authorization

### Data & Persistence
- **PostgreSQL 16** - Relational database for transactional data
- **Hibernate 6.4** - ORM implementation
- **HikariCP** - High-performance connection pooling
- **Flyway** - Database schema migrations (ready for use)

### Messaging & Events
- **Spring Kafka 3.1** - Kafka integration with Spring
- **Apache Kafka 7.5** - Distributed event streaming platform
- **Spring Events** - In-process event publishing

### Security
- **Spring Security 6** - Authentication and authorization framework
- **JWT (JJWT 0.12)** - Token-based authentication
- **BCrypt** - Secure password hashing

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito 5** - Mocking framework for unit tests
- **AssertJ** - Fluent assertion library
- **Testcontainers 1.19** - Integration testing with real dependencies
- **REST Assured** - API testing framework
- **H2** - In-memory database for unit tests

### Observability
- **Micrometer 1.12** - Metrics collection and export
- **Prometheus** - Metrics format and storage
- **SLF4J + Logback** - Structured logging with MDC
- **Spring Boot Actuator** - Health checks, metrics endpoints

### Resilience
- **Resilience4j** - Circuit breaker, retry, rate limiter
- **Spring Retry** - Declarative retry support

### DevOps & Build Tools
- **Maven 3.9.6** - Build automation and dependency management
- **Docker 24** - Containerization
- **Docker Compose** - Multi-container orchestration
- **GitHub Actions** - CI/CD pipeline automation
- **SpotBugs 4.8.3** - Static analysis for bug detection
- **Checkstyle 3.3.1** - Code style enforcement (Google Java Style)
- **Trivy** - Security vulnerability scanning for containers

## 🚀 Getting Started

### Prerequisites

- **Java 17+** ([Download Eclipse Temurin](https://adoptium.net/))
- **Docker & Docker Compose** ([Download](https://www.docker.com/products/docker-desktop))
- **Maven 3.9+** (or use included Maven Wrapper `./mvnw`)
- **Git** ([Download](https://git-scm.com/))

### Quick Start (Docker Compose)

The fastest way to run the entire stack:

```bash
# Clone the repository
git clone https://github.com/Rejennis/order-fulfillment-system.git
cd order-fulfillment-system

# Start all services (PostgreSQL, Kafka, Application)
docker-compose up -d

# View logs
docker-compose logs -f app

# Check health
curl http://localhost:8080/actuator/health

# Access services:
# - Application API: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui.html
# - Kafka UI: http://localhost:8090
# - PgAdmin: http://localhost:5050 (admin@admin.com / admin)
```

Stop all services:

```bash
docker-compose down

# To remove volumes (clean slate):
docker-compose down -v
```

### Local Development Setup

For active development with hot-reload:

```bash
# 1. Start infrastructure only (PostgreSQL, Kafka, Zookeeper)
docker-compose up -d postgres kafka zookeeper

# 2. Build the application
./mvnw clean package -DskipTests

# 3. Run the application
./mvnw spring-boot:run

# Or with specific profile:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Access the application
curl http://localhost:8080/actuator/health
```

### Environment Variables

Key environment variables (see `application.yml` for defaults):

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orderfulfillment
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Security
JWT_SECRET=your-256-bit-secret-key-here
JWT_EXPIRATION_MS=3600000  # 1 hour

# Logging
LOGGING_LEVEL_COM_MIDLEVEL=DEBUG
```

## 📚 API Documentation

### Swagger UI (Interactive Documentation)

Once the application is running, access interactive API documentation:

**http://localhost:8080/swagger-ui.html**

The Swagger UI provides:
- Complete endpoint documentation
- Request/response schemas
- Try-it-out functionality
- Example payloads
- Authentication support

### Authentication Flow

Most endpoints require JWT authentication. Follow these steps:

#### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
  }'

# Response: 201 Created
{
  "message": "User registered successfully"
}
```

#### 2. Login to Get JWT Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!"
  }'

# Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZSIsImlhdCI6MTYxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "type": "Bearer",
  "username": "john.doe",
  "roles": ["ROLE_USER"]
}
```

#### 3. Use Token in Subsequent Requests

```bash
# Save token in variable
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Use in Authorization header
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

### Core API Endpoints

#### Orders API

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| `POST` | `/api/orders` | Create a new order | ✅ | USER |
| `GET` | `/api/orders/{id}` | Get order details | ✅ | USER |
| `GET` | `/api/orders` | List all orders | ✅ | USER |
| `POST` | `/api/orders/{id}/pay` | Process payment | ✅ | USER |
| `POST` | `/api/orders/{id}/ship` | Mark as shipped | ✅ | ADMIN |
| `POST` | `/api/orders/{id}/deliver` | Mark as delivered | ✅ | ADMIN |
| `POST` | `/api/orders/{id}/cancel` | Cancel order | ✅ | USER |

#### Authentication API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | Register new user | ❌ |
| `POST` | `/api/auth/login` | Login and get JWT | ❌ |

#### Monitoring API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/actuator/health` | Health check | ❌ |
| `GET` | `/actuator/metrics` | Available metrics | ❌ |
| `GET` | `/actuator/prometheus` | Prometheus metrics | ❌ |
| `GET` | `/actuator/info` | Application info | ❌ |

### Example API Calls

#### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerName": "John Doe",
    "items": [
      {
        "productId": "PROD-123",
        "productName": "Laptop",
        "quantity": 1,
        "unitPrice": {
          "amount": 999.99,
          "currency": "USD"
        }
      },
      {
        "productId": "PROD-456",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": {
          "amount": 25.00,
          "currency": "USD"
        }
      }
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "postalCode": "62701",
      "country": "USA"
    }
  }'

# Response: 201 Created
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED",
  "customerId": "CUST-001",
  "customerName": "John Doe",
  "items": [
    {
      "productId": "PROD-123",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": { "amount": 999.99, "currency": "USD" },
      "total": { "amount": 999.99, "currency": "USD" }
    },
    {
      "productId": "PROD-456",
      "productName": "Wireless Mouse",
      "quantity": 2,
      "unitPrice": { "amount": 25.00, "currency": "USD" },
      "total": { "amount": 50.00, "currency": "USD" }
    }
  ],
  "total": { "amount": 1049.99, "currency": "USD" },
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "postalCode": "62701",
    "country": "USA"
  },
  "createdAt": "2024-12-26T10:30:00Z",
  "updatedAt": "2024-12-26T10:30:00Z"
}
```

#### Process Payment

```bash
curl -X POST http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/pay \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CREDIT_CARD",
    "amount": {
      "amount": 1049.99,
      "currency": "USD"
    }
  }'

# Response: 200 OK
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PAID",
  "message": "Payment processed successfully",
  "updatedAt": "2024-12-26T10:35:00Z"
}
```

#### Get Order Status

```bash
curl -X GET http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"

# Response: 200 OK
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PAID",
  "customerId": "CUST-001",
  "customerName": "John Doe",
  "total": { "amount": 1049.99, "currency": "USD" },
  "createdAt": "2024-12-26T10:30:00Z",
  "updatedAt": "2024-12-26T10:35:00Z"
}
```

### Error Responses

The API uses standard HTTP status codes and returns consistent error structures:

| Status | Meaning | When |
|--------|---------|------|
| `200` | OK | Success |
| `201` | Created | Resource created |
| `400` | Bad Request | Invalid input data, validation errors |
| `401` | Unauthorized | Missing or invalid JWT token |
| `403` | Forbidden | Valid token but insufficient permissions |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Invalid state transition, business rule violation |
| `500` | Internal Server Error | Unexpected server error |

Example error response:

```json
{
  "timestamp": "2024-12-26T10:40:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot ship order that is not paid. Current status: CREATED",
  "path": "/api/orders/550e8400-e29b-41d4-a716-446655440000/ship",
  "correlationId": "abc123-def456-ghi789"
}
```

## 💻 Development

### Running Tests

```bash
# Run all tests
./mvnw test

# Run only unit tests
./mvnw test -Dtest="*Test"

# Run only integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=OrderServiceTest

# Run with coverage report
./mvnw clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Code Quality Checks

```bash
# Run SpotBugs (static analysis)
./mvnw spotbugs:check

# Run Checkstyle (code style)
./mvnw checkstyle:check

# Run all quality checks
./mvnw verify spotbugs:check checkstyle:check
```

### Building

```bash
# Build JAR (skip tests)
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Build Docker image
docker build -t order-fulfillment-system:latest .

# Build with Docker Compose
docker-compose build
```

### Database Migrations

Database schema is managed by Spring JPA auto-DDL in development. For production, use Flyway:

```bash
# Create migration
# Add SQL file to: src/main/resources/db/migration/V1__initial_schema.sql

# Run migrations
./mvnw flyway:migrate

# Check migration status
./mvnw flyway:info

# Clean database (CAUTION: drops all data)
./mvnw flyway:clean
```

## 🚢 Deployment

### Docker Deployment

#### Single Container

```bash
# Build image
docker build -t order-fulfillment-system:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/orderfulfillment \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e JWT_SECRET=your-secret-key \
  --name order-system \
  order-fulfillment-system:latest

# View logs
docker logs -f order-system

# Stop container
docker stop order-system
```

#### Full Stack with Docker Compose

```bash
# Production deployment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale application instances
docker-compose up -d --scale app=3

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Kubernetes Deployment (Future)

Sample Kubernetes manifests are planned for:
- Deployment with 3 replicas
- Service (LoadBalancer)
- ConfigMap for configuration
- Secret for sensitive data
- HorizontalPodAutoscaler for auto-scaling
- Ingress for external access

### Environment-Specific Configuration

Use Spring profiles for different environments:

```bash
# Development (default)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Production
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Testing
./mvnw test -Dspring.profiles.active=test
```

Configuration files:
- `application.yml` - Default configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides
- `application-test.yml` - Test overrides

## 📊 Observability

### Health Checks

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health with components
curl http://localhost:8080/actuator/health | jq
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "PostgreSQL", "validationQuery": "isValid()" }
    },
    "diskSpace": { "status": "UP" },
    "kafka": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Metrics

```bash
# Available metrics
curl http://localhost:8080/actuator/metrics

# Specific metric (JVM memory)
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Custom business metrics
curl http://localhost:8080/actuator/metrics/orders.created
curl http://localhost:8080/actuator/metrics/orders.paid
curl http://localhost:8080/actuator/metrics/orders.shipped

# Prometheus format (for scraping)
curl http://localhost:8080/actuator/prometheus
```

### Logging

Logs are structured in JSON format with correlation IDs:

```bash
# Tail logs
docker-compose logs -f app

# Filter by correlation ID
docker-compose logs app | grep "correlationId\":\"abc123"

# Filter by log level
docker-compose logs app | grep "ERROR"
```

Example log entry:

```json
{
  "timestamp": "2024-12-26T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.midlevel.orderfulfillment.application.service.OrderService",
  "message": "Order created successfully",
  "correlationId": "abc123-def456",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "orderTotal": 1049.99,
  "thread": "http-nio-8080-exec-1"
}
```

### Distributed Tracing (Planned)

Future integration with:
- **Zipkin** or **Jaeger** for distributed tracing
- **Spring Cloud Sleuth** for trace/span ID propagation
- **OpenTelemetry** for vendor-neutral observability

## 🏆 Project Journey

This project was built over 14 days as part of the "Be Prolific - Gulp Life" Mid-Level Java Developer Mentor Program. Each day focused on a specific aspect of production-ready Java engineering:

### Timeline

| Day | Focus | Key Deliverables |
|-----|-------|------------------|
| 1 | Domain Modeling | Order aggregate, Value objects, State machine |
| 2 | Persistence Layer | JPA entities, Repository, Testcontainers |
| 3 | Architecture Setup | Hexagonal structure, ADRs, Package organization |
| 4 | Business Logic | State transitions, Business rules, Validation |
| 5 | REST API | Controllers, DTOs, HTTP semantics, Error handling |
| 6 | Testing Strategy | Unit tests, Integration tests, Test pyramid |
| 7 | Domain Events | Event publishing, Spring Events, Event listeners |
| 8 | Notifications | Notification port, Async processing, Event handlers |
| 9 | Kafka Integration | Kafka producer/consumer, Retry logic, Idempotency |
| 10 | Observability | Metrics, Structured logging, Health checks |
| 11 | Error Handling | Global handler, Retry, Circuit breaker, Resilience |
| 12 | Security | JWT auth, RBAC, BCrypt, Secured endpoints |
| 13 | Docker & CI/CD | Dockerfile, docker-compose, GitHub Actions, Code quality |
| 14 | Documentation | README, API docs, ADRs, Diagrams, Retrospective |

### Key Learnings

See [docs/retrospective.md](docs/retrospective.md) for detailed retrospective including:
- What went well
- Challenges faced
- Key technical learnings
- What I would do differently
- Next steps for growth

## 📝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards

- Follow Google Java Style Guide
- Write tests for new features
- Keep test coverage above 80%
- Document public APIs with Javadoc
- Update ADRs for architectural changes

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Mentor Program**: "Be Prolific - Gulp Life" by [Mentor Name]
- **Inspiration**: Domain-Driven Design by Eric Evans
- **Architecture**: Hexagonal Architecture by Alistair Cockburn
- **Community**: Spring Boot, Kafka, and open-source communities

## 📧 Contact

**Your Name** - [@yourtwitter](https://twitter.com/yourtwitter)

Project Link: [https://github.com/Rejennis/order-fulfillment-system](https://github.com/Rejennis/order-fulfillment-system)

---

**Built with ❤️ and ☕ as part of the 14-day Mid-Level Java Developer Journey**

*"Ship every day. Done > Perfect. Build real systems, not tutorials."*
