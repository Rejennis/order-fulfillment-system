# Order Fulfillment System - Complete Structure Explained

## 📁 System Architecture Overview

This document provides a comprehensive explanation of every folder and file in the Order Fulfillment System, organized by layer and responsibility.

---

## 🏗️ Root Level Files & Folders

### Configuration & Build Files

#### **`pom.xml`**
Maven project configuration file that defines:
- Project metadata (groupId, artifactId, version)
- Dependencies:
  - Spring Boot 3.2.1 (web, data-jpa, validation)
  - PostgreSQL driver
  - Kafka (spring-kafka, spring-kafka-test) - Day 9
  - Observability (actuator, micrometer, logstash) - Day 10
  - Resilience (spring-retry, resilience4j) - Day 11
  - Testcontainers (PostgreSQL, Kafka)
  - Lombok (reduces boilerplate)
- Java version (17)
- Build plugins (Maven Compiler, Spring Boot Maven Plugin)
- Manages the entire build lifecycle

**Key Dependencies:**
```xml
<!-- Core Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Kafka (Day 9) -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Observability (Day 10) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Error Handling & Resilience (Day 11) -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
    <version>2.1.0</version>
</dependency>
```

---#### **`docker-compose.yml`**
Defines containerized services for local development:

**Database Infrastructure:**
- PostgreSQL 16 database container
- PgAdmin web interface for database management

**Kafka Infrastructure (Day 9 Addition):**
- Zookeeper - Kafka coordination service (port 2181)
- Kafka Broker - Message streaming platform (port 9092)
- Kafka UI - Web-based management interface (port 8090)

**Configuration:**
- Network configuration for service communication
- Volume mappings for data persistence
- Health checks for service readiness
- Resource limits for stability
- Proper startup ordering (Zookeeper → Kafka → Kafka UI)

**Services:**
```yaml
services:
  postgres:
    image: postgres:16
    ports: ["5432:5432"]
    
  pgadmin:
    image: dpage/pgadmin4
    ports: ["5050:80"]
    
  zookeeper:              # Kafka dependency
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]
    
  kafka:                  # Message broker
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]
    
  kafka-ui:               # Management UI
    image: provectuslabs/kafka-ui:latest
    ports: ["8090:8080"]
    depends_on: [kafka]
```

**Access URLs:**
- Application: http://localhost:8080
- PgAdmin: http://localhost:5050
- Kafka UI: http://localhost:8090
- Swagger: http://localhost:8080/swagger-ui.html

---

#### **`.gitignore`**
Specifies which files Git should ignore:
- `target/` - Maven build outputs
- `.idea/` - IntelliJ IDEA project files
- `*.iml` - IntelliJ module files
- OS-specific files

---

### Documentation Files

#### **`README.md`**
Project overview and getting started guide:
- What the system does
- Day 1 deliverable documentation
- Domain model components
- Setup instructions
- Business rules enforced

#### **`DAY_2_SUMMARY.md`**
Day 2: Repository & Persistence Layer
- Spring Boot integration
- JPA entity layer creation
- Repository pattern implementation
- Testcontainers integration tests
- Interview talking points

#### **`DAY_3_SUMMARY.md`**
Day 3: REST API and Service Layer
- REST controller implementation
- DTO creation and mapping
- Service layer orchestration
- API endpoint documentation

#### **`DAY_4_SUMMARY.md`**
Day 4: Additional features and enhancements
- Error handling
- Validation improvements
- Additional business logic

#### **`DAY_8_SUMMARY.md`**
Day 8: Notification System with Async Event-Driven Architecture
- Adapter pattern for notifications
- Async event processing
- Email notification implementation
- Performance optimizations

#### **`Data_Flow.md`**
Complete data flow documentation:
- Step-by-step request journey
- Hexagonal architecture visualization
- State transition diagrams
- Transaction boundaries
- Event-driven patterns

#### **`Data_Flow_Patterns.md`**
Architectural patterns catalog:
- Hexagonal Architecture
- Event-Driven Architecture
- CQRS-lite
- Domain-Driven Design patterns

#### **`GAP_CLOSURE.md`**
Learning gaps identification and closure plan:
- Concepts that need deeper understanding
- Resources for learning
- Practice exercises

#### **`MENTOR_PROGRAM.md`**
Overall mentor program structure:
- 14-day program outline
- Daily objectives
- Learning outcomes
- Project milestones

---

### Documentation Folders

#### **`docs/architecture/`**
Architecture Decision Records (ADRs) documenting key decisions:

- **`adr-001-hexagonal-architecture.md`**
  - Why hexagonal architecture was chosen
  - Benefits and trade-offs
  - Implementation strategy

- **`adr-002-event-driven-notifications.md`**
  - Event-driven design decisions
  - Async processing rationale
  - Decoupling strategy

- **`adr-003-jpa-for-persistence.md`**
  - Why JPA/Hibernate with PostgreSQL
  - ORM benefits
  - Domain/Entity separation rationale

#### **`postman/`**
API testing resources:
- **`Order_Fulfillment_API.postman_collection.json`** - Postman collection with all API endpoints for manual testing

---

### Build Output

#### **`target/`**
Maven-generated build artifacts (not in source control):
- `classes/` - Compiled Java classes
- `test-classes/` - Compiled test classes
- `generated-sources/` - Auto-generated code
- JAR files for deployment

---

## 💻 Source Code Structure (`src/main/java/`)

```
com.midlevel.orderfulfillment/
├── OrderFulfillmentApplication.java    ← Spring Boot entry point (@SpringBootApplication)
├── domain/                             ← CORE: Business logic (framework-free)
│   ├── model/                          ← Domain entities and value objects
│   ├── port/                           ← Port interfaces (contracts)
│   └── event/                          ← Domain events
├── application/                        ← Service orchestration layer
├── adapter/                            ← Infrastructure adapters
│   ├── in/                            ← Inbound adapters (receive requests)
│   │   └── web/                       ← REST API
│   └── out/                           ← Outbound adapters (external systems)
│       ├── persistence/               ← Database
│       ├── event/                     ← Event listeners
│       └── notification/              ← Email/SMS
└── config/                            ← Spring configuration classes
```

---

## 🎯 Domain Layer (`domain/`) - The Heart of the System

**Purpose:** Contains pure business logic with NO framework dependencies. This is where business rules live.

### `domain/model/` - Domain Models (Pure Java)

#### **`Order.java`** - Aggregate Root
The central entity that manages the complete order lifecycle:
- **Factory Method:** `create()` - Creates new orders with validation
- **State Transitions:** `pay()`, `ship()`, `cancel()`
- **Business Rules Enforcement:**
  - Must have at least one item
  - Total must be greater than zero
  - Valid state transitions only
  - Cannot ship unpaid orders
  - Cannot cancel shipped orders
- **Event Registration:** Registers domain events internally
- **Encapsulation:** No public setters, immutable where possible

#### **`OrderItem.java`** - Value Object
Represents a single item in an order:
- Product ID and name
- Unit price (Money)
- Quantity
- Calculates line total (price × quantity)
- Immutable - once created, cannot change
- Factory method for creation

#### **`OrderStatus.java`** - Enum (State Machine)
Defines valid order states:
- `CREATED` - Order placed
- `PAID` - Payment confirmed
- `SHIPPED` - Order dispatched
- `CANCELLED` - Order cancelled
- Contains state transition validation logic
- Prevents illegal state transitions

#### **`Money.java`** - Value Object
Represents monetary amounts with currency:
- Uses `BigDecimal` for precision (avoids floating-point errors)
- Currency code (USD, EUR, etc.)
- Arithmetic operations: `add()`, `multiply()`
- Immutable and self-validating
- Prevents negative amounts

#### **`Address.java`** - Value Object
Represents shipping address:
- Street, city, state, zip code, country
- Validation logic for address format
- Immutable

---

### `domain/port/` - Port Interfaces (Contracts)

**Purpose:** Interfaces defined by the domain that infrastructure must implement (Dependency Inversion Principle)

#### **`OrderRepository.java`**
Repository contract for order persistence:
- `save(Order order)` - Persist order
- `findById(String orderId)` - Retrieve by ID
- `findByCustomerId(String customerId)` - Get customer orders
- `findByStatus(OrderStatus status)` - Query by status
- `findAll()` - Get all orders
- Domain defines WHAT it needs, not HOW to get it

#### **`NotificationPort.java`**
Notification contract for sending messages:
- `notifyOrderCreated(Order order)`
- `notifyOrderPaid(Order order)`
- `notifyOrderShipped(Order order)`
- `notifyOrderCancelled(Order order)`
- Domain doesn't care about email, SMS, or push notifications

---

### `domain/event/` - Domain Events

**Purpose:** Events that signal important business occurrences

#### **`DomainEvent.java`**
Base interface for all domain events:
- Marker interface
- Defines common event properties

#### **`OrderCreatedEvent.java`**
Published when a new order is created:
- Order ID
- Customer ID
- Total amount
- Item count
- Timestamp

#### **`OrderPaidEvent.java`**
Published when payment succeeds:
- Order ID
- Payment timestamp
- Payment method

#### **`OrderShippedEvent.java`**
Published when order ships:
- Order ID
- Shipping timestamp
- Tracking number

#### **`OrderCancelledEvent.java`**
Published when order is cancelled:
- Order ID
- Cancellation reason
- Timestamp

---

## 🎭 Application Layer (`application/`) - Orchestration

**Purpose:** Coordinates workflows, manages transactions, and publishes events. No business rules here—just coordination.

### **`OrderService.java`** - Main Service Class
Orchestrates all order operations:
- **`createOrder(Order order)`** - Creates new order
  - Validates input
  - Saves to repository
  - Publishes events
  - Returns created order
- **`markOrderAsPaid(String orderId)`** - Processes payment
- **`markOrderAsShipped(String orderId)`** - Ships order
- **`cancelOrder(String orderId)`** - Cancels order
- **`findById(String orderId)`** - Retrieves order
- Uses `@Transactional` for ACID guarantees
- Delegates business logic to domain
- Coordinates persistence and event publishing

### **`DomainEventPublisher.java`** - Event Publishing Coordination
Publishes domain events after transactions commit:
- Extracts registered events from aggregates
- Delegates to appropriate publisher implementation
- Ensures events only publish on successful commits
- Prevents inconsistent state (event without data)

### **`EventPublisher.java`** - Event Publisher Interface (Day 9 Addition)
Port interface for event publishing implementations:
```java
public interface EventPublisher {
    void publish(DomainEvent event);
}
```

### **`SpringEventPublisher.java`** - Spring Events Implementation
In-memory event publishing for monoliths:
- Uses Spring's `ApplicationEventPublisher`
- Fast (~1ms latency)
- Events lost if JVM crashes
- Same transaction boundary
- Good for: Monoliths, simple apps, prototyping

### **`DualEventPublisher.java`** - Dual Publishing Strategy (Day 9 Addition)

**Purpose:** Allows toggling between Spring Events and Kafka based on configuration

```java
@Component
@RequiredArgsConstructor
public class DualEventPublisher implements EventPublisher {
    private final SpringEventPublisher springEventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;
    
    @Value("${events.publisher:spring}")
    private String publisherType;
    
    @Override
    public void publish(DomainEvent event) {
        if ("kafka".equalsIgnoreCase(publisherType)) {
            kafkaEventPublisher.publish(event);
        } else {
            springEventPublisher.publish(event);
        }
    }
}
```

**Key Benefits:**
1. **Easy A/B Testing:** Switch between implementations via config
2. **Gradual Migration:** Move from Spring Events to Kafka incrementally
3. **Development Flexibility:** Use Spring Events locally, Kafka in production
4. **Adapter Pattern:** Domain doesn't know which implementation is used

**Configuration:**
```yaml
events:
  publisher: kafka  # or "spring" for in-memory events
```

**When to Use Each:**
- `spring`: Local development, simple apps, fast prototyping
- `kafka`: Production, microservices, audit requirements, event sourcing

---

### **`NotificationService.java`** - Notification Coordination
High-level notification orchestration:
- Uses `NotificationPort` to send notifications
- Called by event listeners
- Formats notification messages
- Handles notification failures gracefully

---

## 🔌 Adapter Layer (`adapter/`) - Infrastructure

**Purpose:** Connects the domain to external systems (HTTP, databases, events). All framework-specific code lives here.

---

## Inbound Adapters (`adapter/in/`) - Receive Requests

### `adapter/in/web/` - REST API Layer

#### **`OrderController.java`** - REST Controller
Exposes HTTP endpoints:
- `POST /api/orders` - Create order
  - Accepts `CreateOrderRequest` DTO
  - Validates with `@Valid`
  - Returns 201 CREATED
- `POST /api/orders/{id}/pay` - Mark as paid
  - Returns 200 OK
- `POST /api/orders/{id}/ship` - Mark as shipped
  - Returns 200 OK
- `POST /api/orders/{id}/cancel` - Cancel order
  - Returns 200 OK
- `GET /api/orders/{id}` - Get order details
  - Returns 200 OK or 404 NOT FOUND
- `GET /api/orders` - List all orders
- `GET /api/orders/customer/{customerId}` - Get customer orders
- Uses `@RestController`, `@RequestMapping` annotations
- Maps DTOs ↔ Domain models via mapper

#### **`GlobalExceptionHandler.java`** - Centralized Exception Handling
Converts exceptions to HTTP responses:
- `@ExceptionHandler` methods for different exception types
- Returns consistent error format
- Sets appropriate HTTP status codes
- Logs errors for debugging

---

### `adapter/in/web/dto/` - Data Transfer Objects (DTOs)

**Purpose:** External API representation that is completely separate from internal domain models

#### 🎯 What Are DTOs and Why Do We Need Them?

**DTOs (Data Transfer Objects)** are simple data containers used to transfer data between different layers of an application, especially across network boundaries (like REST APIs). They are the "translation layer" between the external world (JSON, XML) and your internal domain model.

**The Problem DTOs Solve:**

Imagine exposing your domain model directly as your API:
```java
// BAD: Exposing domain directly
@PostMapping("/api/orders")
public Order createOrder(@RequestBody Order order) {  // ❌ WRONG!
    return orderService.save(order);
}
```

**Why this is problematic:**
1. **Tight Coupling:** API structure is tied to domain structure
   - Can't change domain without breaking API
   - Can't change API without changing domain
2. **Security Risks:** Might expose internal fields clients shouldn't see
   - Database IDs, internal states, sensitive data
3. **Lack of Control:** Can't add API-specific validation
4. **Version Hell:** Can't support multiple API versions
5. **Serialization Issues:** Domain objects might have circular references, lazy-loading issues

**The Solution: DTOs as a Translation Layer**

```
External World (JSON)  ←→  DTO  ←→  Mapper  ←→  Domain Model
     {"customerId": "123"}     CreateOrderRequest     OrderDtoMapper     Order(aggregate)
```

DTOs create a **protective barrier** around your domain, allowing:
- API to evolve independently of domain
- Domain to evolve independently of API
- Different representations for different consumers
- API-specific validation and formatting

---

#### 📦 **`CreateOrderRequest.java`** - Inbound Request DTO

**Purpose:** Represents the JSON structure clients send when creating an order

**What it contains:**
```java
public record CreateOrderRequest(
    @NotBlank String customerId,              // Customer placing order
    @Valid AddressDto shippingAddress,        // Where to ship
    @NotEmpty List<OrderItemRequest> items    // What they're ordering
) {
    public record OrderItemRequest(
        @NotBlank String productId,
        @NotBlank String productName,
        @Valid MoneyDto unitPrice,
        @NotNull Integer quantity
    ) {}
}
```

**Key Features:**

1. **Validation Annotations** - API-level validation happens HERE, not in domain:
   - `@NotBlank` - String cannot be null, empty, or just whitespace
   - `@NotNull` - Field cannot be null
   - `@NotEmpty` - Collection must have at least one element
   - `@Valid` - Recursively validate nested objects
   - Spring automatically validates BEFORE the request reaches your controller

2. **Immutable Record** - Java 17+ records provide:
   - Automatic constructor, getters, equals(), hashCode(), toString()
   - Immutability by default (thread-safe)
   - Concise syntax

3. **API Contract** - This is what clients must send:
   ```json
   {
     "customerId": "customer-123",
     "shippingAddress": {
       "street": "123 Main St",
       "city": "Springfield",
       "state": "IL",
       "zipCode": "62701",
       "country": "US"
     },
     "items": [
       {
         "productId": "prod-456",
         "productName": "Widget",
         "unitPrice": {"amount": 29.99, "currency": "USD"},
         "quantity": 2
       }
     ]
   }
   ```

4. **Decoupled from Domain** - If domain `Order` changes (adds fields, renames properties), this DTO stays the same → API doesn't break

**When to use:** For every API endpoint that accepts input

---

#### 📤 **`OrderResponse.java`** - Outbound Response DTO

**Purpose:** Represents the JSON structure sent back to clients after operations

**What it contains:**
```java
public record OrderResponse(
    String orderId,                    // Unique identifier
    String customerId,                 // Who placed it
    String status,                     // Current state (CREATED, PAID, SHIPPED)
    Instant createdAt,                // When created
    Instant paidAt,                   // When paid (null if not yet)
    Instant shippedAt,                // When shipped (null if not yet)
    AddressDto shippingAddress,       // Where it's going
    List<OrderItemResponse> items,    // What's in the order
    MoneyDto totalAmount              // Total cost
) {
    public record OrderItemResponse(
        String productId,
        String productName,
        MoneyDto unitPrice,
        Integer quantity,
        MoneyDto lineTotal      // quantity × unitPrice
    ) {}
}
```

**Key Features:**

1. **Client-Friendly Format:**
   - Status as string (not enum) for easier parsing
   - Timestamps as ISO-8601 (Spring Boot auto-converts)
   - Nested objects for clarity
   - Calculated fields (lineTotal, totalAmount)

2. **Complete Information:**
   - Everything client needs to display order
   - No need for additional requests
   - Includes computed values

3. **Example Response:**
   ```json
   {
     "orderId": "ord-789",
     "customerId": "customer-123",
     "status": "CREATED",
     "createdAt": "2025-12-26T10:30:00Z",
     "paidAt": null,
     "shippedAt": null,
     "shippingAddress": {
       "street": "123 Main St",
       "city": "Springfield",
       "state": "IL",
       "zipCode": "62701",
       "country": "US"
     },
     "items": [
       {
         "productId": "prod-456",
         "productName": "Widget",
         "unitPrice": {"amount": 29.99, "currency": "USD"},
         "quantity": 2,
         "lineTotal": {"amount": 59.98, "currency": "USD"}
       }
     ],
     "totalAmount": {"amount": 59.98, "currency": "USD"}
   }
   ```

4. **Versioning Support:**
   - Can create `OrderResponseV2` for new API version
   - Old clients keep using `OrderResponse`
   - Both can map from same domain `Order`

**When to use:** For every API endpoint that returns data

---

#### 🧩 **`AddressDto.java`** - Nested Value DTO

**Purpose:** Reusable DTO for addresses (used in both requests and responses)

```java
public record AddressDto(
    String street,
    String city,
    String state,
    String zipCode,
    String country
) {}
```

**Why separate?**
- Used in multiple places (CreateOrderRequest, OrderResponse)
- DRY principle (Don't Repeat Yourself)
- Easy to add address validation in one place

---

#### 💰 **`MoneyDto.java`** - Value DTO for Money

**Purpose:** Represents monetary amounts with currency

```java
public record MoneyDto(
    BigDecimal amount,
    String currency     // ISO 4217 code (USD, EUR, GBP)
) {}
```

**Why not just a number?**
- Money always has a currency context
- Prevents mixing currencies
- Explicit representation = fewer bugs

---

#### ❌ **`ErrorResponse.java`** - Error DTO

**Purpose:** Consistent error format for all API errors

```java
public record ErrorResponse(
    String message,        // Human-readable error
    int status,           // HTTP status code
    String timestamp,     // When error occurred
    String path          // Which endpoint failed
) {}
```

**Example:**
```json
{
  "message": "Order not found",
  "status": 404,
  "timestamp": "2025-12-26T10:30:00Z",
  "path": "/api/orders/invalid-id"
}
```

**Benefits:**
- Clients know what to expect
- Easier error handling
- Consistent across all endpoints

---

### `adapter/in/web/mapper/` - The Translation Layer

#### 🔄 **`OrderDtoMapper.java`** - DTO ↔ Domain Converter

**Purpose:** Translates between DTOs (external representation) and Domain Models (internal representation)

**Why a separate mapper class?**

1. **Separation of Concerns:**
   - DTOs know about JSON structure
   - Domain knows about business rules
   - Mapper knows about translation
   - Each has ONE job

2. **Testability:**
   - Can unit test mapping logic
   - Easy to verify translations
   - No need to start Spring

3. **Flexibility:**
   - Support multiple API versions with different DTOs
   - Same domain model, different external views
   - Easy to add new mappings

4. **Maintainability:**
   - All mapping logic in one place
   - Easy to find and update
   - Clear responsibility

---

#### 📥 **Inbound Mapping: DTO → Domain**

**Method:** `toDomain(CreateOrderRequest) → Order`

**What it does:**
```java
public Order toDomain(CreateOrderRequest request) {
    // 1. Convert DTO items to domain OrderItems
    var items = request.items().stream()
            .map(this::toOrderItem)    // Convert each item
            .collect(Collectors.toList());
    
    // 2. Convert DTO address to domain Address
    Address address = toAddress(request.shippingAddress());
    
    // 3. Use domain factory method (enforces business rules)
    return Order.create(
            request.customerId(),
            items,
            address
    );
}
```

**Step-by-step translation:**
1. **Extract data from DTO** - Get customerId, items, address
2. **Convert nested DTOs** - OrderItemRequest → OrderItem, AddressDto → Address
3. **Call domain factory** - `Order.create()` enforces validation
4. **Return domain object** - Ready for business logic

**Key point:** Mapper calls domain factory methods, ensuring validation happens in the domain, not in the mapper.

**Example flow:**
```
Client JSON
    ↓
CreateOrderRequest (DTO) - "What client sent"
    ↓
OrderDtoMapper.toDomain()
    ↓
Order (Domain) - "Business entity with rules"
    ↓
OrderService (processes business logic)
```

---

#### 📤 **Outbound Mapping: Domain → DTO**

**Method:** `toResponse(Order) → OrderResponse`

**What it does:**
```java
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
            order.getOrderId(),
            order.getCustomerId(),
            order.getStatus().name(),        // Enum → String
            order.getCreatedAt(),
            order.getPaidAt(),
            order.getShippedAt(),
            toAddressDto(order.getShippingAddress()),
            order.getItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList()),
            toMoneyDto(order.calculateTotal())   // Call domain method
    );
}
```

**Step-by-step translation:**
1. **Extract domain data** - Get all fields from Order
2. **Convert enums to strings** - `OrderStatus.CREATED` → `"CREATED"`
3. **Convert nested domain objects** - OrderItem → OrderItemResponse, Address → AddressDto
4. **Call domain calculations** - `calculateTotal()` for total amount
5. **Build response DTO** - Package everything for client

**Example flow:**
```
Order (Domain) - "Rich business entity"
    ↓
OrderDtoMapper.toResponse()
    ↓
OrderResponse (DTO) - "Simplified view for client"
    ↓
JSON sent to client
```

---

#### 🔍 **Helper Mapping Methods**

The mapper includes helper methods for converting nested objects:

**`toOrderItem(OrderItemRequest)`** - Convert item DTO to domain
```java
private OrderItem toOrderItem(CreateOrderRequest.OrderItemRequest dto) {
    Money unitPrice = toMoney(dto.unitPrice());
    return OrderItem.create(
            dto.productId(),
            dto.productName(),
            unitPrice,
            dto.quantity()
    );
}
```

**`toOrderItemResponse(OrderItem)`** - Convert item domain to DTO
```java
private OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item) {
    return new OrderResponse.OrderItemResponse(
            item.getProductId(),
            item.getProductName(),
            toMoneyDto(item.getUnitPrice()),
            item.getQuantity(),
            toMoneyDto(item.calculateLineTotal())    // Domain calculation
    );
}
```

**`toAddress(AddressDto)` / `toAddressDto(Address)`** - Address conversions
**`toMoney(MoneyDto)` / `toMoneyDto(Money)`** - Money conversions

---

#### 🎯 **Key Principles of DTOs and Mappers**

1. **DTOs are DUMB** - No business logic, just data holders
   - No validation beyond format (@NotNull, @NotBlank)
   - No calculations
   - No state management

2. **Domain is SMART** - All business logic lives here
   - Validation: "Order must have items"
   - Calculations: "Total = sum of line totals"
   - Rules: "Can't ship unpaid orders"

3. **Mapper is the BRIDGE** - Just translates, no decisions
   - Knows both DTO and Domain structure
   - Performs mechanical conversion
   - Delegates to domain for creation/validation

4. **Direction Matters:**
   - **Inbound (DTO → Domain):** Use domain factory methods
   - **Outbound (Domain → DTO):** Simple field copying

5. **Testability:**
   ```java
   @Test
   void shouldMapRequestToDomain() {
       // Given
       CreateOrderRequest request = new CreateOrderRequest(...);
       
       // When
       Order order = mapper.toDomain(request);
       
       // Then
       assertThat(order.getCustomerId()).isEqualTo(request.customerId());
       assertThat(order.getItems()).hasSize(request.items().size());
   }
   ```

---

#### 💡 **Real-World Benefits**

**Scenario 1: API Version Change**
```java
// API v1 uses OrderResponse
// API v2 needs additional field (estimated delivery)
public record OrderResponseV2(
    // ... all OrderResponse fields ...
    Instant estimatedDelivery    // NEW field
) {}

// Add new mapper method
public OrderResponseV2 toResponseV2(Order order) {
    // ... map everything + calculate delivery estimate
}

// Domain unchanged! ✅
```

**Scenario 2: Domain Refactoring**
```java
// Domain changes: OrderStatus enum renamed values
// Before: CREATED, PAID, SHIPPED
// After: PENDING, CONFIRMED, DISPATCHED

// Mapper absorbs the change:
public OrderResponse toResponse(Order order) {
    String status = switch(order.getStatus()) {
        case PENDING -> "CREATED";      // API still says "CREATED"
        case CONFIRMED -> "PAID";       // API still says "PAID"
        case DISPATCHED -> "SHIPPED";   // API still says "SHIPPED"
    };
    return new OrderResponse(/* ... */, status, /* ... */);
}

// API contract unchanged! ✅
```

**Scenario 3: Security**
```java
// Domain Order has internal fields:
class Order {
    private String internalAuditLog;      // Should NOT be exposed
    private BigDecimal costPrice;          // Trade secret
    private String warehouseLocation;      // Internal info
    // ... public fields ...
}

// DTO only exposes what clients should see:
public record OrderResponse(
    String orderId,
    String customerId,
    // ... only public information ...
    // ❌ NO internal fields!
) {}

// Mapper only copies safe fields ✅
```

---

#### 📚 **DTO Pattern: Summary**

**Three-Layer Structure:**
```
┌──────────────────────────────────────┐
│  DTOs (adapter/in/web/dto/)         │  ← External API contract
│  - CreateOrderRequest                │
│  - OrderResponse                     │
│  - AddressDto, MoneyDto              │
└──────────┬───────────────────────────┘
           │
           │ OrderDtoMapper translates
           │
┌──────────▼───────────────────────────┐
│  Domain (domain/model/)              │  ← Business logic & rules
│  - Order (Aggregate Root)            │
│  - OrderItem, Money, Address         │
│  - Business methods & validation     │
└──────────────────────────────────────┘
```

**Why This Matters:**
- **Stability:** API doesn't break when domain changes
- **Security:** Control what's exposed
- **Flexibility:** Support multiple API versions
- **Clarity:** Clear boundary between external and internal
- **Testability:** Each layer tests independently

**Alternative: MapStruct**
For complex projects, consider [MapStruct](https://mapstruct.org/) - generates mapper code at compile time, reducing boilerplate while keeping the benefits of DTOs.

---

## Outbound Adapters (`adapter/out/`) - External Interactions

### `adapter/out/persistence/` - Database Adapter

#### **`OrderRepositoryAdapter.java`** - Repository Implementation
Implements `OrderRepository` port:
- Converts `Order` (domain) ↔ `OrderEntity` (JPA)
- Delegates to `JpaOrderRepository` for database operations
- Bridge between domain and infrastructure
- **Key Methods:**
  - `save(Order)` - Converts to entity, saves, converts back
  - `findById(String)` - Queries DB, converts to domain
  - `findByCustomerId(String)` - Custom query
  - `findByStatus(OrderStatus)` - Custom query

#### **`JpaOrderRepository.java`** - Spring Data JPA Interface
Extends `JpaRepository<OrderEntity, String>`:
- Auto-generates SQL queries
- Provides CRUD operations
- Custom query methods:
  - `findByCustomerId(String)`
  - `findByStatus(OrderStatus)`
- Uses Spring Data JPA magic (no implementation needed)

---

### `adapter/out/persistence/entity/` - JPA Entities

**Purpose:** Database representation with JPA annotations (separate from domain models)

#### **`OrderEntity.java`** - JPA Entity for `orders` table
Maps to database table:
- `@Entity` annotation
- `@Table(name = "orders")`
- `@Id` for primary key
- `@OneToMany` relationship with OrderItemEntity
- `@Embedded` for AddressEmbeddable
- **Static Methods:**
  - `fromDomain(Order)` - Converts domain → entity
  - `toDomain()` - Converts entity → domain
- Contains JPA-specific annotations and relationships

#### **`OrderItemEntity.java`** - JPA Entity for `order_items` table
Maps to order items table:
- `@Entity` annotation
- `@GeneratedValue` for auto-increment ID
- `@ManyToOne` relationship with OrderEntity
- Foreign key to orders table
- Conversion methods to/from domain

#### **`AddressEmbeddable.java`** - Embedded Address
Embedded in OrderEntity (not a separate table):
- `@Embeddable` annotation
- Street, city, state, zip, country columns
- Part of orders table structure

---

### `adapter/out/event/` - Event Listener (Spring Events)

#### **`OrderEventListener.java`** - Domain Event Listener
Listens to and reacts to domain events:
- `@EventListener` annotation
- `@Async` - Runs in separate thread
- **Event Handlers:**
  - `handleOrderCreated(OrderCreatedEvent)` - Logs, notifies customer, reserves inventory
  - `handleOrderPaid(OrderPaidEvent)` - Sends payment confirmation, notifies warehouse
  - `handleOrderShipped(OrderShippedEvent)` - Sends shipping notification
  - `handleOrderCancelled(OrderCancelledEvent)` - Sends cancellation email, releases inventory
- Non-blocking - doesn't delay main request
- Runs outside transaction boundary
- Can trigger multiple actions per event

---

### `adapter/out/kafka/` - Kafka Event Streaming (Day 9 Addition)

#### **`KafkaEventPublisher.java`** - Kafka Producer Adapter

**Purpose:** Publishes domain events to Kafka topics for distributed event streaming

**Key Features:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "events.publisher", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Override
    public void publish(DomainEvent event) {
        String topic = determineTopicFromEvent(event);
        String key = extractKeyFromEvent(event);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = 
            kafkaTemplate.send(topic, key, event);
            
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, partition={}, offset={}", 
                    topic, result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event to topic={}", topic, ex);
            }
        });
    }
}
```

**How It Works:**
1. **Topic Routing:** Maps event type to Kafka topic
   - `OrderCreatedEvent` → `order.created`
   - `OrderPaidEvent` → `order.paid`
   - `OrderShippedEvent` → `order.shipped`
   - `OrderCancelledEvent` → `order.cancelled`

2. **Partition Key:** Uses `orderId` to ensure ordering
   - All events for same order go to same partition
   - Maintains event ordering per order

3. **Async Publishing:** Non-blocking with callbacks
   - Success: Logs partition and offset
   - Failure: Logs error (can route to DLQ)

**Configuration:**
```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                    # Wait for all replicas
      enable-idempotence: true     # Prevent duplicates
```

---

#### **`KafkaEventConsumer.java`** - Kafka Consumer Adapter

**Purpose:** Consumes events from Kafka topics and triggers business logic

**Key Features:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {
    private final NotificationPort notificationPort;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
    
    @KafkaListener(
        topics = {"order.created", "order.paid", "order.shipped", "order.cancelled"},
        groupId = "order-fulfillment-service"
    )
    @Transactional
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        Acknowledgment acknowledgment
    ) {
        // Idempotency check
        if (processedEventIds.contains(event.getEventId())) {
            log.info("Duplicate event ignored: {}", event.getEventId());
            acknowledgment.acknowledge();
            return;
        }
        
        try {
            // Process event
            notificationPort.notifyOrderCreated(event.getOrder());
            processedEventIds.add(event.getEventId());
            acknowledgment.acknowledge();  // Manual commit
        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventId(), e);
            // Don't acknowledge - Kafka will retry
        }
    }
}
```

**How It Works:**
1. **Idempotency:** Tracks processed events to prevent duplicates
   - Uses in-memory `Set<String>` for demo
   - Production should use Redis/Database

2. **Manual Acknowledgment:** Controls when offset is committed
   - Success → Acknowledge (commit offset)
   - Failure → Don't acknowledge (Kafka retries)

3. **Transactional Processing:** Ensures DB + notification consistency

4. **Multiple Topics:** Single listener for all order events
   - Can separate into specialized consumers later

**Configuration:**
```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      group-id: order-fulfillment-service
      auto-offset-reset: earliest     # Start from beginning
      enable-auto-commit: false       # Manual commits
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.midlevel.orderfulfillment.domain.event"
```

---

#### **Comparison: Spring Events vs Kafka**

| Feature | Spring Events | Kafka |
|---------|--------------|-------|
| **Scope** | In-memory (single JVM) | Distributed (multiple services) |
| **Persistence** | No (lost on restart) | Yes (retained on disk) |
| **Ordering** | No guarantees | Guaranteed per partition |
| **Scalability** | Limited to one instance | Horizontal scaling |
| **Reliability** | Lost if process crashes | Durable, replicated |
| **Latency** | ~1ms | ~5-10ms |
| **Use Case** | Monolith, internal events | Microservices, event sourcing |
| **Transaction** | Same transaction | Separate transaction |

**When to Use What:**
- **Spring Events:** Simple apps, same JVM, fast prototyping
- **Kafka:** Microservices, audit trail, event sourcing, high volume

---

### `adapter/out/notification/` - Notification Adapter

#### **`EmailNotificationAdapter.java`** - Email Implementation
Implements `NotificationPort` interface:
- Sends actual emails via SMTP or email service
- Formats email templates
- Handles email failures
- Can be swapped for SMS, push notifications, etc.
- **Methods:**
  - `notifyOrderCreated(Order)` - Sends "Order Confirmation" email
  - `notifyOrderPaid(Order)` - Sends "Payment Confirmed" email
  - `notifyOrderShipped(Order)` - Sends "Order Shipped" email with tracking
  - `notifyOrderCancelled(Order)` - Sends "Order Cancelled" email

---

## ⚙️ Config Layer (`config/`) - Spring Configuration

### **`AsyncConfig.java`** - Async Processing Configuration
Configures asynchronous execution:
- Defines thread pool for `@Async` methods
- Sets pool size (core threads, max threads)
- Queue capacity for pending tasks
- Thread naming strategy
- Rejection policy when pool is full
- Enables `@EnableAsync` annotation

### **`KafkaConfig.java`** - Kafka Infrastructure Configuration (Day 9 Addition)

**Purpose:** Defines Kafka topics and infrastructure settings

```java
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Topic Definitions
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
    
    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name("order.paid")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name("order.shipped")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("order.events.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
```

**Key Concepts:**

1. **Topics:** Named message queues
   - `order.created` - New order events
   - `order.paid` - Payment confirmation events
   - `order.shipped` - Shipping notification events
   - `order.cancelled` - Cancellation events
   - `order.events.dlq` - Dead Letter Queue for failed messages

2. **Partitions:** Enable parallel processing
   - 3 partitions per topic = 3 consumers can process in parallel
   - Messages with same key go to same partition (ordering guarantee)
   - More partitions = better scalability

3. **Replication Factor:** Data durability
   - `replicas(1)` - Development/Demo setup
   - Production should use `replicas(3)` for fault tolerance
   - Each replica is a backup copy on different broker

4. **Cleanup Policy:**
   - `CLEANUP_POLICY_COMPACT` - Keeps latest value per key
   - Useful for event sourcing (keep latest order state)
   - `CLEANUP_POLICY_DELETE` - Time-based retention (default)

5. **Dead Letter Queue (DLQ):**
   - Stores messages that fail after all retries
   - Allows manual inspection and reprocessing
   - Prevents message loss

**Production Considerations:**
```java
// Production topic configuration
@Bean
public NewTopic orderCreatedTopicProduction() {
    return TopicBuilder.name("order.created")
            .partitions(10)          // More partitions for scale
            .replicas(3)             // 3 replicas for fault tolerance
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")  // Wait for 2 replicas
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")  // 7 days retention
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy") // Compress messages
            .build();
}
```

---

### **`OpenApiConfig.java`** - API Documentation Configuration
Configures Swagger/OpenAPI:
- API metadata (title, version, description)
- Contact information
- License information
- Server URLs
- Generates interactive API docs at `/swagger-ui.html`
- Enables easy API testing

---

## 📦 Resources (`src/main/resources/`)

### **`application.yml`** - Main Application Configuration
Spring Boot configuration:
```yaml
server:
  port: 8080

spring:
  application:
    name: order-fulfillment-system
  
  datasource:
    url: jdbc:postgresql://localhost:5432/orderfulfillment
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create tables
    show-sql: true      # Log SQL queries
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  hikari:  # Connection pool
    maximum-pool-size: 10
    minimum-idle: 5
    connection-timeout: 30000

  # Kafka Configuration (Day 9 Addition)
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                    # Wait for all replicas to acknowledge
      retries: 3                   # Retry on failure
      enable-idempotence: true     # Prevent duplicates
    consumer:
      group-id: order-fulfillment-service
      auto-offset-reset: earliest  # Start from beginning if no offset
      enable-auto-commit: false    # Manual offset commits
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.midlevel.orderfulfillment.domain.event"

# Event Publisher Configuration (Day 9 Addition)
events:
  publisher: kafka  # Options: "spring" (in-memory) or "kafka" (distributed)
```

**Key Configuration Explained:**

**Kafka Producer Settings:**
- `acks: all` - Wait for all replicas before confirming send (durability)
- `enable-idempotence: true` - Kafka ensures exactly-once delivery
- `JsonSerializer` - Auto-converts Java objects to JSON

**Kafka Consumer Settings:**
- `group-id` - Consumer group for load balancing
- `auto-offset-reset: earliest` - Start from beginning if no saved offset
- `enable-auto-commit: false` - Manual control over when to commit offsets
- `trusted.packages` - Security: only deserialize trusted classes

**Event Publisher Toggle:**
- `events.publisher: spring` - Uses in-memory Spring Events
- `events.publisher: kafka` - Uses Kafka distributed events
- Allows easy A/B testing and gradual migration

---

### **`application-test.yml`** - Test Configuration
Test-specific settings:
- Uses Testcontainers for integration tests
- In-memory H2 database for unit tests
- Overrides main configuration during tests
- Test logging levels

---

## 🧪 Test Structure (`src/test/java/`)

Mirrors the main source structure with comprehensive tests:

### **`domain/model/`** - Domain Unit Tests
Fast, isolated tests with no Spring:
- `OrderTest.java` - Tests Order aggregate logic
  - State transitions
  - Business rule validation
  - Factory methods
  - Edge cases
- `MoneyTest.java` - Tests Money value object
- `OrderItemTest.java` - Tests OrderItem logic
- **No database, no Spring - just pure Java**

### **`application/`** - Service Layer Tests
Tests service orchestration:
- `OrderServiceTest.java` - Tests OrderService
  - Mocks repository
  - Verifies transaction handling
  - Tests event publishing

### **`adapter/in/web/`** - Controller Tests
Integration tests with MockMvc:
- `OrderControllerTest.java` - Tests REST endpoints
  - HTTP request/response
  - DTO validation
  - Status codes
  - JSON serialization

### **`adapter/out/persistence/`** - Repository Tests
Integration tests with real database:
- `OrderRepositoryIntegrationTest.java`
  - Uses Testcontainers (PostgreSQL in Docker)
  - Tests CRUD operations
  - Verifies JPA mappings
  - Tests custom queries
  - Validates relationships and cascades
  - **12 comprehensive tests**

---

## 🏛️ Architecture Pattern: Hexagonal (Ports & Adapters)

```
┌───────────────────────────────────────────────────────────┐
│                   INBOUND ADAPTERS                         │
│  (How the outside world talks to us)                      │
│  - OrderController (REST API)                             │
│  - DTOs (CreateOrderRequest, OrderResponse)               │
│  - Mappers (OrderDtoMapper)                               │
└──────────────────────┬────────────────────────────────────┘
                       ↓
┌───────────────────────────────────────────────────────────┐
│                APPLICATION LAYER                           │
│  (Orchestrates workflows, manages transactions)           │
│  - OrderService (coordinates operations)                  │
│  - DomainEventPublisher (publishes events)                │
│  - NotificationService (notification coordination)        │
└──────────────────────┬────────────────────────────────────┘
                       ↓
┌───────────────────────────────────────────────────────────┐
│                  DOMAIN LAYER (CORE)                       │
│  (Pure business logic - no frameworks)                    │
│  - Order (Aggregate Root)                                 │
│  - OrderRepository (Port Interface)                       │
│  - NotificationPort (Port Interface)                      │
│  - Money, OrderItem, Address (Value Objects)              │
│  - Domain Events (OrderCreatedEvent, etc.)                │
└──────────────────────┬────────────────────────────────────┘
                       ↓
┌───────────────────────────────────────────────────────────┐
│                  OUTBOUND ADAPTERS                         │
│  (How we talk to the outside world)                       │
│  - OrderRepositoryAdapter (implements OrderRepository)    │
│  - JpaOrderRepository (Spring Data JPA)                   │
│  - OrderEntity, OrderItemEntity (JPA entities)            │
│  - EmailNotificationAdapter (implements NotificationPort) │
│  - OrderEventListener (reacts to domain events)           │
└───────────────────────────────────────────────────────────┘
```

---

## 🔑 Key Architectural Principles

### 1. Dependency Inversion Principle
- Domain defines ports (interfaces)
- Infrastructure implements adapters
- **Dependencies point INWARD** toward the domain
- Domain never depends on infrastructure

### 2. Separation of Concerns
- **Domain:** Business rules only
- **Application:** Workflow coordination
- **Adapters:** Technology-specific code
- Each layer has a single, clear responsibility

### 3. Testability
- Domain can be tested without Spring
- Services can be tested with mocks
- Adapters tested with real infrastructure
- Tests are fast and reliable

### 4. Flexibility
- Swap PostgreSQL for MongoDB → only change persistence adapter
- Swap REST for GraphQL → only change web adapter
- Swap Email for SMS → only change notification adapter
- **Domain remains unchanged**

### 5. Event-Driven Design
- Domain events decouple reactions from actions
- Async processing improves performance
- Easy to add new listeners without changing existing code
- Eventual consistency for non-critical operations

---

## 🎯 Summary

This Order Fulfillment System demonstrates:
- ✅ **Hexagonal Architecture** (Ports & Adapters)
- ✅ **Domain-Driven Design** (Aggregates, Value Objects, Events)
- ✅ **Event-Driven Architecture** (Async event processing)
- ✅ **CQRS-lite** (Separate command and query paths)
- ✅ **Clean Architecture** (Framework-independent domain)
- ✅ **SOLID Principles** (Especially Dependency Inversion)
- ✅ **Comprehensive Testing** (Unit, Integration, API tests)
- ✅ **Production-Ready Patterns** (Transaction management, error handling, async processing)

The architecture enables:
- Fast, isolated unit tests
- Easy technology swapping
- Clear separation of concerns
- Maintainable, scalable codebase
- Business logic that lives in the domain, not scattered across layers

---

## 🚀 Day 9 Additions: Kafka Event Streaming

The system now supports both **Spring Events (in-memory)** and **Kafka (distributed)** for event-driven architecture.

### New Components Added

**Infrastructure:**
- docker-compose.yml - Added Zookeeper, Kafka broker, Kafka UI
- pom.xml - Added spring-kafka dependencies

**Configuration:**
- application.yml - Kafka producer/consumer config and event publisher toggle
- KafkaConfig.java - Topic definitions (order.created, order.paid, order.shipped, order.cancelled, DLQ)

**Adapters:**
- KafkaEventPublisher.java - Producer publishing events to Kafka
- KafkaEventConsumer.java - Consumer with idempotency and manual acknowledgment
- DualEventPublisher.java - Toggle between Spring Events/Kafka via config

### Architecture Evolution

**Before (Day 8):**
```
OrderService → SpringEventPublisher → OrderEventListener
              (in-memory)              (async processing)
```

**After (Day 9):**
```
OrderService → DualEventPublisher → [SpringEventPublisher OR KafkaEventPublisher]
                                     ↓                        ↓
                                 OrderEventListener    Kafka Topics (order.*)
                                                             ↓
                                                      KafkaEventConsumer
```

### Key Benefits

**Flexibility:**
- Toggle between implementations via config: `events.publisher: spring` or `kafka`
- No code changes needed to switch modes
- Perfect for gradual migration or A/B testing

**Scalability:**
- Kafka enables horizontal scaling across multiple service instances
- Partitioning ensures order-level event ordering
- Consumer groups enable parallel processing

**Reliability:**
- Kafka persists events to disk (survives restarts/crashes)
- Replication provides fault tolerance
- Manual acknowledgment prevents message loss
- Dead Letter Queue (DLQ) for failed messages

**Observability:**
- Kafka UI (http://localhost:8090) for real-time monitoring
- View topics, partitions, messages, consumer lag
- Debug production issues with message replay

For complete details, see DAY_9_SUMMARY.md and KAFKA_QUICKSTART.md.

---

## 📊 Day 10 Additions: Observability & Monitoring

Production systems need visibility into their health and behavior. Day 10 adds comprehensive observability infrastructure.

### New Components Added

**Dependencies:**
- spring-boot-starter-actuator - Production-ready endpoints
- micrometer-registry-prometheus - Metrics export
- logstash-logback-encoder - JSON structured logging

**Configuration:**
- application.yml - Actuator endpoints, health indicators, metrics config
- logback-spring.xml - Dual profile logging (dev: human-readable, prod: JSON)

**Infrastructure:**
- CorrelationIdFilter.java - HTTP filter for request tracing across services
- MetricsConfiguration.java - 8 custom business metrics beans
- KafkaHealthIndicator.java - Custom health check for Kafka connectivity

**Enhanced Components:**
- OrderService.java - Metrics counters and timers on all operations
- KafkaEventPublisher.java - Event publishing metrics and correlation ID propagation
- KafkaEventConsumer.java - Event consumption metrics with MDC context
- OrderController.java - Request/response logging
- NotificationService.java - Notification metrics

### Observability Capabilities

**Health Checks:**
- `/actuator/health` - Overall system health (UP/DOWN)
- Custom Kafka health indicator with 5s timeout
- Database, disk space, and dependency health checks

**Metrics (Prometheus format):**
- `/actuator/prometheus` - All metrics in Prometheus format
- `/actuator/metrics/{metric.name}` - Individual metric details

**Business Metrics:**
```
orders.created.total          - Total orders created
orders.failures.total         - Order operation failures
orders.status.changes.total   - Status transition count
orders.creation.duration      - Order creation time (histogram)
events.published.total        - Events published to Kafka
events.consumed.total         - Events consumed from Kafka
events.failures.total         - Event processing failures
notifications.sent.total      - Notifications sent
```

**Correlation ID Tracing:**
- Every request gets a unique correlation ID (X-Correlation-Id header)
- ID flows through: HTTP → Service → Database → Kafka → Consumers
- Enables request tracing across distributed components
- Stored in MDC (Mapped Diagnostic Context) for logging

**Structured Logging:**
- **Dev profile**: Human-readable console logs with correlation IDs
- **Prod profile**: JSON logs with all context (timestamp, level, logger, correlationId, etc.)
- 30-day retention with daily rotation
- Easy parsing for log aggregation tools (ELK, Splunk)

### Architecture Evolution

**Before (Day 9):**
```
Order Creation → Database → Kafka → Notifications
(No visibility into performance or failures)
```

**After (Day 10):**
```
HTTP Request → CorrelationIdFilter (adds ID)
    ↓
OrderController (logs request with correlation ID)
    ↓
OrderService (metrics: counter++, timer.record())
    ↓
Database (transaction traced with correlation ID)
    ↓
KafkaEventPublisher (metrics, correlation ID in headers)
    ↓
KafkaEventConsumer (MDC context, metrics)
    ↓
NotificationService (metrics)
```

### Key Benefits

**Debugging:**
- Trace single request across all components using correlation ID
- Identify slow operations with timing histograms
- Find failures with error counters

**Monitoring:**
- Prometheus scrapes `/actuator/prometheus` for dashboards (Grafana)
- Alert on high failure rates, slow response times
- Track business KPIs (orders/hour, event lag)

**Production Readiness:**
- Health checks for load balancer routing
- Structured logs for centralized logging (ELK stack)
- Metrics for capacity planning and SLA tracking

For complete details, see DAY_10_COMPLETION_SUMMARY.md.

---

## 🛡️ Day 11 Additions: Error Handling & Resilience

Production systems must handle failures gracefully. Day 11 adds three critical resilience patterns.

### New Components Added

**Dependencies:**
- spring-retry - Automatic retry for transient failures
- spring-boot-starter-aop - Enables @Retryable annotations
- resilience4j-spring-boot3 - Circuit breaker pattern
- resilience4j-micrometer - Circuit breaker metrics

**Configuration:**
- RetryConfiguration.java - Enables @Retryable support
- CircuitBreakerConfiguration.java - Default circuit breaker config

**Error Handling:**
- GlobalExceptionHandler.java - Centralized REST API error handling with RFC 7807 Problem Details

**Enhanced Components:**
- OrderService.java - @Retryable on 4 methods with @Recover fallback
- KafkaEventPublisher.java - Circuit breaker wrapping for Kafka calls

### Resilience Patterns Implemented

#### 1. Global Exception Handling (RFC 7807 Problem Details)

**Purpose:** Consistent, machine-readable error responses across all endpoints

**Handles:**
- `OrderNotFoundException` → HTTP 404
- `IllegalStateException` → HTTP 400 (invalid state transitions)
- `IllegalArgumentException` → HTTP 400 (invalid input)
- `MethodArgumentNotValidException` → HTTP 400 (validation errors with field details)
- Generic `Exception` → HTTP 500 (unexpected errors)

**Error Response Format:**
```json
{
  "type": "https://api.orderfulfillment.com/errors/order-not-found",
  "title": "Order Not Found",
  "status": 404,
  "detail": "Order not found: order-123",
  "timestamp": "2024-12-26T16:55:00Z"
}
```

#### 2. Retry Pattern (Spring Retry)

**Purpose:** Automatic recovery from transient database failures

**Applied to OrderService methods:**
- `createOrder()` - Order creation
- `markOrderAsPaid()` - Payment confirmation
- `markOrderAsShipped()` - Shipping update
- `cancelOrder()` - Order cancellation

**Retry Strategy:**
```java
@Retryable(
    retryFor = DataAccessException.class,  // Only retry database failures
    maxAttempts = 3,                        // 1 initial + 2 retries
    backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s, 4s
)
```

**Recovery Method:**
```java
@Recover
public Order recoverFromCreateOrder(DataAccessException e, Order order) {
    log.error("All retry attempts exhausted...");
    throw new RuntimeException("Order creation failed after multiple attempts", e);
}
```

**Retries:**
- Database connection timeouts
- Optimistic locking conflicts
- Temporary network issues

**Does NOT retry:**
- Business rule violations (invalid state transitions)
- Validation errors (missing required fields)
- Authorization failures

#### 3. Circuit Breaker Pattern (Resilience4j)

**Purpose:** Prevent cascade failures when Kafka is unavailable

**Circuit Breaker Config:**
```java
failureRateThreshold: 50%          // Open after 50% failures
slowCallRateThreshold: 50%         // Open after 50% slow calls (>5s)
waitDurationInOpenState: 30s       // Wait before testing recovery
permittedCallsInHalfOpen: 3        // Test with 3 calls
minimumNumberOfCalls: 5            // Need 5 calls to calculate rates
slidingWindowSize: 10              // Track last 10 calls
```

**State Machine:**
1. **CLOSED** (normal): All Kafka publish attempts go through
2. **OPEN** (failure): After 50% failures, fail fast without trying Kafka
3. **HALF_OPEN** (testing): After 30s, allow 3 test calls
4. **Back to CLOSED**: If test calls succeed, resume normal operation

**Applied to KafkaEventPublisher:**
```java
circuitBreaker.executeRunnable(() -> {
    kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> { /* handle result */ });
});
```

**Benefits:**
- Orders can still be created even if Kafka is down
- Prevents thread pool exhaustion from waiting on timeouts
- Automatic recovery testing without manual intervention
- State transitions logged for observability

### Architecture Evolution

**Before (Day 10):**
```
HTTP Request → OrderService → Database (fails on connection timeout)
                ↓
           OrderService → Kafka (blocks if Kafka down)
```

**After (Day 11):**
```
HTTP Request → GlobalExceptionHandler wraps everything
                ↓
         OrderService with @Retryable (auto-retry DB failures)
                ↓
         Database (retries: 1s, 2s, 4s on failure)
                ↓
         @Recover fallback if all retries fail
                ↓
         KafkaEventPublisher with CircuitBreaker
                ↓
         Kafka (fails fast when circuit OPEN, auto-recovers)
```

### Key Benefits

**Reliability:**
- Automatic recovery from transient failures (database connection issues)
- Graceful degradation (orders work even if Kafka is down)
- Prevents cascade failures (circuit breaker stops trying when system is down)

**User Experience:**
- Consistent error messages with proper HTTP status codes
- Field-level validation feedback
- Fewer user-facing errors from transient issues

**Observability:**
- Circuit breaker state transitions logged
- Retry attempts logged with correlation IDs
- Metrics for failure rates, slow calls, circuit state

**Production Readiness:**
- Handles database connection pool exhaustion
- Prevents Kafka unavailability from blocking order processing
- Industry-standard error format (RFC 7807) for API consumers

For complete details, see DAY_11_SUMMARY.md.

---

## 🎯 Complete System Summary

This Order Fulfillment System now demonstrates:
- ✅ **Hexagonal Architecture** (Ports & Adapters)
- ✅ **Domain-Driven Design** (Aggregates, Value Objects, Events)
- ✅ **Event-Driven Architecture** (Spring Events + Kafka)
- ✅ **CQRS-lite** (Separate command and query paths)
- ✅ **Clean Architecture** (Framework-independent domain)
- ✅ **SOLID Principles** (Especially Dependency Inversion)
- ✅ **Comprehensive Testing** (Unit, Integration, API tests with Testcontainers)
- ✅ **Observability** (Metrics, structured logging, correlation IDs, health checks) - Day 10
- ✅ **Resilience Patterns** (Retry, circuit breaker, global exception handling) - Day 11
- ✅ **Production-Ready** (Transaction management, error handling, monitoring, failure recovery)

### Complete Request Flow

```
1. HTTP Request arrives
   ↓ CorrelationIdFilter (adds X-Correlation-Id)
   
2. OrderController (logs request)
   ↓ GlobalExceptionHandler (wraps for consistent errors)
   
3. OrderService (with @Retryable)
   ↓ Metrics: counter++, timer.record()
   ↓ Transaction boundary starts
   
4. Database Operation
   ↓ Retry on failure: 1s, 2s, 4s
   ↓ @Recover fallback if all fail
   
5. Transaction commits
   ↓ Domain events registered
   
6. DualEventPublisher (Spring or Kafka)
   ↓ If Kafka: KafkaEventPublisher
   ↓ Circuit breaker wrapped
   ↓ Fail fast if circuit OPEN
   
7. Kafka Topic (order.created, etc.)
   ↓ Persistent, replicated, durable
   
8. KafkaEventConsumer
   ↓ MDC context with correlation ID
   ↓ Idempotency check
   ↓ Manual acknowledgment
   
9. NotificationService
   ↓ Async email/SMS
   ↓ Metrics: notifications.sent.total++
   
10. Response to client
    ↓ Correlation ID in response header
    ↓ RFC 7807 format on errors
```

### Observability Endpoints

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Kafka UI**: http://localhost:8090 (when Docker running)
- **PgAdmin**: http://localhost:5050 (when Docker running)

The architecture enables:
- Fast, isolated unit tests
- Easy technology swapping (Kafka ↔ Spring Events)
- Clear separation of concerns
- Maintainable, scalable codebase
- Business logic in domain, not scattered across layers
- Full production observability and resilience
- Graceful failure handling and automatic recovery

