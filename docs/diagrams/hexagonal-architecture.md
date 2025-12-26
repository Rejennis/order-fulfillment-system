# Hexagonal Architecture Diagram

## Overview
This system follows **Hexagonal Architecture** (Ports & Adapters pattern) to achieve clean separation of concerns, testability, and independence from external frameworks.

## Architecture Visualization

```mermaid
graph TB
    subgraph "External World"
        REST[REST API Clients]
        KAFKA_IN[Kafka Producers]
        DB[(PostgreSQL Database)]
        KAFKA_OUT[Kafka Consumers]
    end
    
    subgraph "Adapters Layer (Infrastructure)"
        subgraph "Inbound Adapters (Driving)"
            REST_ADAPTER[REST Controllers<br/>OrderController<br/>AuthController]
            KAFKA_LISTENER[Kafka Listeners<br/>NotificationListener]
        end
        
        subgraph "Outbound Adapters (Driven)"
            JPA_ADAPTER[JPA Repositories<br/>OrderJpaAdapter<br/>UserJpaAdapter]
            KAFKA_PUBLISHER[Kafka Publishers<br/>NotificationPublisher]
            EVENT_PUBLISHER[Spring Event Publisher<br/>OrderEventPublisher]
        end
    end
    
    subgraph "Domain Layer (Core Business Logic)"
        DOMAIN_MODEL[Domain Model<br/>Order<br/>OrderItem<br/>User]
        DOMAIN_SERVICE[Domain Services<br/>OrderService<br/>PaymentService]
        DOMAIN_EVENTS[Domain Events<br/>OrderCreatedEvent<br/>OrderPaidEvent]
    end
    
    subgraph "Application Layer (Use Cases)"
        USE_CASES[Use Cases<br/>CreateOrderUseCase<br/>ProcessPaymentUseCase<br/>ShipOrderUseCase]
        APP_EVENTS[Event Handlers<br/>OrderEventHandler]
    end
    
    subgraph "Ports (Interfaces)"
        subgraph "Inbound Ports"
            IN_PORT[Command Ports<br/>OrderCommandPort<br/>AuthCommandPort]
        end
        
        subgraph "Outbound Ports"
            OUT_PORT[Repository Ports<br/>OrderRepositoryPort<br/>UserRepositoryPort<br/>EventPublisherPort]
        end
    end
    
    %% External to Adapters
    REST -->|HTTP| REST_ADAPTER
    KAFKA_IN -->|Messages| KAFKA_LISTENER
    DB -.->|Persist| JPA_ADAPTER
    KAFKA_OUT -.->|Publish| KAFKA_PUBLISHER
    
    %% Inbound Flow
    REST_ADAPTER -->|Calls| IN_PORT
    KAFKA_LISTENER -->|Calls| IN_PORT
    
    %% Ports to Application
    IN_PORT -->|Delegates| USE_CASES
    
    %% Application to Domain
    USE_CASES -->|Uses| DOMAIN_SERVICE
    USE_CASES -->|Creates| DOMAIN_MODEL
    USE_CASES -->|Publishes| DOMAIN_EVENTS
    
    %% Domain to Ports
    USE_CASES -->|Calls| OUT_PORT
    APP_EVENTS -->|Calls| OUT_PORT
    
    %% Outbound Flow
    OUT_PORT -->|Implemented by| JPA_ADAPTER
    OUT_PORT -->|Implemented by| KAFKA_PUBLISHER
    OUT_PORT -->|Implemented by| EVENT_PUBLISHER
    
    %% Event Flow
    DOMAIN_EVENTS -->|Handled by| APP_EVENTS
    APP_EVENTS -->|Triggers| OUT_PORT

    style DOMAIN_MODEL fill:#e1f5e1
    style DOMAIN_SERVICE fill:#e1f5e1
    style DOMAIN_EVENTS fill:#e1f5e1
    style USE_CASES fill:#e1e5f5
    style APP_EVENTS fill:#e1e5f5
    style IN_PORT fill:#fff4e1
    style OUT_PORT fill:#fff4e1
    style REST_ADAPTER fill:#f5e1e1
    style JPA_ADAPTER fill:#f5e1e1
```

## Layer Responsibilities

### 🎯 Domain Layer (Core)
**Location:** `com.midlevel.orderfulfillment.domain`

**Purpose:** Contains pure business logic with zero framework dependencies

**Components:**
- **Domain Model** (`domain.model`): Entities with rich behavior
  - `Order`: Order aggregate with state transitions
  - `OrderItem`: Value object for line items
  - `User`: User aggregate with authentication
  - `OrderStatus`, `PaymentStatus`: Enumerations
  
- **Domain Services** (`domain.service`): Complex business operations
  - `OrderService`: Order lifecycle management
  - `PaymentService`: Payment processing logic
  
- **Domain Events** (`domain.event`): Business event definitions
  - `OrderCreatedEvent`
  - `OrderPaidEvent`
  - `OrderShippedEvent`
  - `OrderDeliveredEvent`

**Key Principle:** No dependencies on frameworks, databases, or external services

---

### 🔌 Application Layer (Use Cases)
**Location:** `com.midlevel.orderfulfillment.application`

**Purpose:** Orchestrates domain logic and coordinates between adapters

**Components:**
- **Use Cases** (`application.usecase`): Specific user actions
  - `CreateOrderUseCase`: Create new order
  - `ProcessPaymentUseCase`: Process order payment
  - `ShipOrderUseCase`: Mark order as shipped
  - `DeliverOrderUseCase`: Mark order as delivered
  
- **Event Handlers** (`application.event`): React to domain events
  - `OrderEventHandler`: Publishes to Kafka on order events

**Key Principle:** Framework-agnostic orchestration of business logic

---

### 🎭 Adapters Layer (Infrastructure)
**Location:** `com.midlevel.orderfulfillment.adapter`

**Purpose:** Connects external world to domain through ports

#### Inbound Adapters (Driving)
Receive requests from external sources:
- **REST Controllers** (`adapter.rest`): HTTP API endpoints
  - `OrderController`: Order CRUD operations
  - `AuthController`: Authentication endpoints
  
- **Kafka Listeners** (`adapter.messaging.kafka.listener`): Message consumers
  - `NotificationListener`: Consumes notification events

#### Outbound Adapters (Driven)
Implement persistence and external communication:
- **JPA Repositories** (`adapter.persistence.jpa`): Database operations
  - `OrderJpaAdapter`: Implements `OrderRepositoryPort`
  - `UserJpaAdapter`: Implements `UserRepositoryPort`
  
- **Kafka Publishers** (`adapter.messaging.kafka.publisher`): Event publishing
  - `NotificationPublisher`: Publishes to Kafka topics
  
- **Event Publishers** (`adapter.event`): Spring Events
  - `OrderEventPublisher`: Publishes domain events

**Key Principle:** All framework-specific code lives here

---

### 🔗 Ports (Interfaces)
**Location:** `com.midlevel.orderfulfillment.domain.port`

**Purpose:** Define contracts between layers

#### Inbound Ports (API)
Define what the application can do:
- `OrderCommandPort`: Order operations
- `AuthCommandPort`: Authentication operations

#### Outbound Ports (SPI)
Define what the application needs:
- `OrderRepositoryPort`: Order persistence
- `UserRepositoryPort`: User persistence
- `EventPublisherPort`: Event publishing

**Key Principle:** Interfaces owned by domain, implemented by adapters

---

## Dependency Rule

The fundamental rule: **Dependencies point inward**

```
External World → Adapters → Ports → Application → Domain
```

- ✅ Domain depends on **NOTHING**
- ✅ Application depends on **Domain only**
- ✅ Ports depend on **Domain only**
- ✅ Adapters depend on **Ports and Application**
- ❌ Domain **NEVER** depends on Adapters
- ❌ Application **NEVER** depends on Adapters

---

## Benefits Realized

### 🧪 Testability
- Domain logic tested in isolation without database/frameworks
- Use case testing with mock adapters
- Integration tests at adapter boundaries

### 🔄 Flexibility
- Swap PostgreSQL for MongoDB without touching domain
- Replace Kafka with RabbitMQ with minimal changes
- Add GraphQL alongside REST without domain changes

### 🎯 Focus
- Business logic clearly separated from technical concerns
- Easy to understand what the system does (domain layer)
- Infrastructure changes don't ripple through business code

### 📦 Maintainability
- Clear separation of concerns
- Changes localized to specific layers
- New features added without breaking existing code

---

## Example: Order Creation Flow

```mermaid
sequenceDiagram
    participant Client
    participant REST as REST Adapter
    participant Port as Inbound Port
    participant UseCase as Application Layer
    participant Domain
    participant OutPort as Outbound Port
    participant JPA as JPA Adapter
    participant DB as Database

    Client->>REST: POST /api/orders
    REST->>Port: createOrder(request)
    Port->>UseCase: execute(command)
    UseCase->>Domain: new Order(...)
    Domain-->>UseCase: order
    UseCase->>OutPort: save(order)
    OutPort->>JPA: save(orderEntity)
    JPA->>DB: INSERT INTO orders
    DB-->>JPA: success
    JPA-->>OutPort: order
    OutPort-->>UseCase: order
    UseCase->>Domain: publishEvent(OrderCreated)
    Domain-->>UseCase: event published
    UseCase-->>Port: OrderResponse
    Port-->>REST: OrderResponse
    REST-->>Client: 201 Created
```

---

## References
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
