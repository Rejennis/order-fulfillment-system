# Data Flow Diagrams

## 1. Order Creation Flow

Complete flow from REST request to database persistence and event publishing.

```mermaid
sequenceDiagram
    actor User
    participant REST as OrderController
    participant Auth as SecurityFilter
    participant UseCase as CreateOrderUseCase
    participant Domain as OrderService
    participant Entity as Order
    participant RepoPort as OrderRepositoryPort
    participant JPA as OrderJpaAdapter
    participant DB as PostgreSQL
    participant EventPub as OrderEventPublisher
    participant Kafka as Kafka Broker

    User->>REST: POST /api/orders<br/>{customerId, items[]}
    REST->>Auth: validateJWT(token)
    Auth-->>REST: UserDetails
    
    REST->>UseCase: createOrder(request, userId)
    UseCase->>Domain: createOrder(customerId, items)
    
    Domain->>Entity: new Order(customerId)
    Entity-->>Domain: order (CREATED)
    
    Domain->>Entity: addItem(item) for each
    Entity->>Entity: validate business rules
    Entity-->>Domain: order with items
    
    Domain-->>UseCase: order
    
    UseCase->>RepoPort: save(order)
    RepoPort->>JPA: save(orderEntity)
    JPA->>JPA: mapToEntity(order)
    JPA->>DB: INSERT INTO orders
    JPA->>DB: INSERT INTO order_items
    DB-->>JPA: success
    JPA->>JPA: mapToDomain(entity)
    JPA-->>RepoPort: order
    RepoPort-->>UseCase: order
    
    UseCase->>EventPub: publish(OrderCreatedEvent)
    EventPub->>Kafka: send(order-events, event)
    Kafka-->>EventPub: ack
    EventPub-->>UseCase: success
    
    UseCase-->>REST: OrderResponse
    REST-->>User: 201 Created<br/>{id, status, total}
```

---

## 2. Payment Processing Flow

Flow showing idempotent payment processing with state validation.

```mermaid
sequenceDiagram
    actor User
    participant REST as OrderController
    participant UseCase as ProcessPaymentUseCase
    participant Domain as PaymentService
    participant Order as Order Entity
    participant Repo as OrderRepositoryPort
    participant DB as PostgreSQL
    participant Events as Spring Events
    participant KafkaHandler as OrderEventHandler
    participant Kafka as Kafka Broker

    User->>REST: POST /api/orders/{id}/pay<br/>{amount, method}
    REST->>UseCase: processPayment(orderId, amount)
    
    UseCase->>Repo: findById(orderId)
    Repo->>DB: SELECT * FROM orders WHERE id=?
    DB-->>Repo: order row
    Repo-->>UseCase: order
    
    UseCase->>Domain: processPayment(order, amount)
    
    Domain->>Order: validateStatus()
    alt Order already PAID
        Order-->>Domain: already paid (idempotent)
        Domain-->>UseCase: success (no-op)
        UseCase-->>REST: 200 OK (already paid)
    else Order is CREATED
        Order->>Order: status = PAID
        Order->>Order: paymentDate = now()
        Order-->>Domain: order updated
        
        Domain->>Order: publishEvent(OrderPaidEvent)
        Order-->>Domain: event
        Domain-->>UseCase: order + event
        
        UseCase->>Repo: save(order)
        Repo->>DB: UPDATE orders SET status=PAID
        DB-->>Repo: success
        Repo-->>UseCase: order
        
        UseCase->>Events: publishEvent(OrderPaidEvent)
        Events->>KafkaHandler: onOrderPaid(event)
        KafkaHandler->>Kafka: send(order-paid-topic)
        Kafka-->>KafkaHandler: ack
        KafkaHandler-->>Events: handled
        Events-->>UseCase: published
        
        UseCase-->>REST: OrderResponse
        REST-->>User: 200 OK<br/>{id, status=PAID}
    end
```

---

## 3. Order Shipping Flow

Admin-only operation with role-based authorization.

```mermaid
sequenceDiagram
    actor Admin
    participant REST as OrderController
    participant Auth as SecurityFilter
    participant UseCase as ShipOrderUseCase
    participant Domain as OrderService
    participant Order as Order Entity
    participant Repo as OrderRepositoryPort
    participant DB as PostgreSQL
    participant Events as Spring Events
    participant Kafka as Kafka Broker

    Admin->>REST: POST /api/orders/{id}/ship
    REST->>Auth: validateJWT(token)
    Auth->>Auth: checkRole(ADMIN)
    alt Not ADMIN
        Auth-->>REST: 403 Forbidden
        REST-->>Admin: 403 Forbidden
    else Is ADMIN
        Auth-->>REST: authorized
        
        REST->>UseCase: shipOrder(orderId, adminId)
        
        UseCase->>Repo: findById(orderId)
        Repo->>DB: SELECT * FROM orders
        DB-->>Repo: order
        Repo-->>UseCase: order
        
        UseCase->>Domain: shipOrder(order)
        Domain->>Order: validateCanShip()
        
        alt Order not PAID
            Order-->>Domain: error: must be paid first
            Domain-->>UseCase: BusinessException
            UseCase-->>REST: 400 Bad Request
            REST-->>Admin: Cannot ship unpaid order
        else Order is PAID
            Order->>Order: status = SHIPPED
            Order->>Order: shippedDate = now()
            Order-->>Domain: order updated
            
            Domain->>Order: publishEvent(OrderShippedEvent)
            Domain-->>UseCase: order + event
            
            UseCase->>Repo: save(order)
            Repo->>DB: UPDATE orders SET status=SHIPPED
            DB-->>Repo: success
            Repo-->>UseCase: order
            
            UseCase->>Events: publish(OrderShippedEvent)
            Events->>Kafka: send(order-shipped-topic)
            Kafka-->>Events: ack
            Events-->>UseCase: published
            
            UseCase-->>REST: OrderResponse
            REST-->>Admin: 200 OK<br/>{id, status=SHIPPED}
        end
    end
```

---

## 4. Event-Driven Notification Flow

Asynchronous event publishing and consumption.

```mermaid
flowchart TB
    Start([Order Status Change])
    
    DomainEvent[Domain Event Published<br/>OrderCreatedEvent<br/>OrderPaidEvent<br/>OrderShippedEvent]
    
    SpringEvent[Spring Event Handler<br/>OrderEventHandler]
    
    KafkaPublish[Kafka Publisher<br/>NotificationPublisher]
    
    KafkaTopic[(Kafka Topic<br/>order-events)]
    
    KafkaConsumer[Kafka Consumer<br/>NotificationListener]
    
    LogNotification[Log Notification<br/>to Console]
    
    EmailService[Email Service<br/>Future Integration]
    
    SMSService[SMS Service<br/>Future Integration]
    
    Start --> DomainEvent
    DomainEvent --> SpringEvent
    SpringEvent --> KafkaPublish
    KafkaPublish --> KafkaTopic
    KafkaTopic --> KafkaConsumer
    KafkaConsumer --> LogNotification
    KafkaConsumer -.-> EmailService
    KafkaConsumer -.-> SMSService
    
    style DomainEvent fill:#e1f5e1
    style SpringEvent fill:#e1e5f5
    style KafkaPublish fill:#fff4e1
    style KafkaTopic fill:#ffe1e1
    style KafkaConsumer fill:#e1f5f5
    style EmailService fill:#f0f0f0,stroke-dasharray: 5 5
    style SMSService fill:#f0f0f0,stroke-dasharray: 5 5
```

---

## 5. Authentication Flow

User registration and JWT token generation.

```mermaid
sequenceDiagram
    actor User
    participant REST as AuthController
    participant UseCase as RegisterUserUseCase
    participant Domain as UserService
    participant Encoder as BCryptEncoder
    participant Repo as UserRepositoryPort
    participant DB as PostgreSQL
    participant JWT as JwtTokenProvider

    User->>REST: POST /api/auth/register<br/>{username, password, email}
    
    REST->>UseCase: registerUser(request)
    
    UseCase->>Repo: existsByUsername(username)
    Repo->>DB: SELECT COUNT(*) FROM users
    DB-->>Repo: count
    Repo-->>UseCase: exists
    
    alt Username exists
        UseCase-->>REST: 400 Bad Request
        REST-->>User: Username already taken
    else Username available
        UseCase->>Domain: createUser(username, password, email)
        
        Domain->>Encoder: encode(password)
        Encoder-->>Domain: hashedPassword
        
        Domain->>Domain: new User(username, hash, email)
        Domain-->>UseCase: user
        
        UseCase->>Repo: save(user)
        Repo->>DB: INSERT INTO users
        DB-->>Repo: user with id
        Repo-->>UseCase: user
        
        UseCase->>JWT: generateToken(user)
        JWT->>JWT: create claims<br/>(userId, username, roles)
        JWT->>JWT: sign with secret
        JWT-->>UseCase: token
        
        UseCase-->>REST: AuthResponse<br/>{token, username}
        REST-->>User: 201 Created<br/>{token: "eyJ..."}
    end
```

---

## 6. Query Flow (Read Operation)

Optimized read path with minimal layers.

```mermaid
flowchart LR
    User([User])
    REST[OrderController<br/>getOrderById]
    Repo[OrderRepositoryPort]
    JPA[OrderJpaAdapter]
    DB[(PostgreSQL)]
    
    User -->|GET /api/orders/{id}| REST
    REST -->|findById| Repo
    Repo -->|query| JPA
    JPA -->|SELECT| DB
    DB -->|order entity| JPA
    JPA -->|map to domain| Repo
    Repo -->|order| REST
    REST -->|200 OK| User
    
    style User fill:#e1e5f5
    style REST fill:#fff4e1
    style Repo fill:#e1f5e1
    style JPA fill:#ffe1e1
    style DB fill:#f5e1e1
```

---

## Key Data Flow Patterns

### 🔄 Command Flow (Write Operations)
```
REST → UseCase → Domain Service → Entity → Repository → Database
                           ↓
                   Domain Events → Event Handler → Kafka
```

### 📖 Query Flow (Read Operations)
```
REST → Repository → JPA Adapter → Database
```

### 📨 Event Flow (Asynchronous)
```
Domain Event → Spring Event Bus → Handler → Kafka Publisher → Kafka Topic → Consumer
```

### 🔐 Security Flow
```
Request → JWT Filter → Validate Token → Extract User → SecurityContext → Controller
```

---

## Data Transformation Points

### 1. REST → Command (DTO)
```java
CreateOrderRequest (DTO) → CreateOrderCommand (Application)
```

### 2. Command → Domain
```java
CreateOrderCommand → Order Entity (Domain)
```

### 3. Domain → Entity (JPA)
```java
Order (Domain) → OrderEntity (JPA)
```

### 4. Entity → Response
```java
Order (Domain) → OrderResponse (DTO)
```

---

## Error Handling Flow

```mermaid
flowchart TB
    Request[Incoming Request]
    Controller[REST Controller]
    UseCase[Use Case]
    Domain[Domain Service]
    
    BusinessEx[Business Exception<br/>InvalidStateException]
    ValidationEx[Validation Exception<br/>InvalidInputException]
    NotFoundEx[Not Found Exception<br/>EntityNotFoundException]
    
    GlobalHandler[GlobalExceptionHandler<br/>@ControllerAdvice]
    
    ErrorResponse[Error Response<br/>{code, message, timestamp}]
    
    Request --> Controller
    Controller --> UseCase
    UseCase --> Domain
    
    Domain -.-> BusinessEx
    UseCase -.-> NotFoundEx
    Controller -.-> ValidationEx
    
    BusinessEx --> GlobalHandler
    NotFoundEx --> GlobalHandler
    ValidationEx --> GlobalHandler
    
    GlobalHandler --> ErrorResponse
    
    style BusinessEx fill:#ffe1e1
    style ValidationEx fill:#ffe1e1
    style NotFoundEx fill:#ffe1e1
    style GlobalHandler fill:#fff4e1
    style ErrorResponse fill:#e1e5f5
```

---

## Performance Considerations

### Database Queries
- **N+1 Problem Prevention**: Use `@EntityGraph` for order items
- **Pagination**: All list endpoints support page/size parameters
- **Indexing**: Indexes on customerId, status, createdDate

### Event Publishing
- **Asynchronous**: Events published asynchronously via `@Async`
- **Non-blocking**: Kafka producer configured for async sends
- **Resilience**: Retries and circuit breakers on Kafka publisher

### Caching Strategy (Future)
```
GET /api/orders/{id} → Check Cache → Database if miss → Update Cache
```

---

## References
- [Spring Boot Data Flow Best Practices](https://spring.io/guides)
- [Kafka Patterns](https://www.confluent.io/blog/event-driven-architecture-apache-kafka/)
- [Domain Events Pattern](https://martinfowler.com/eaaDev/DomainEvent.html)
