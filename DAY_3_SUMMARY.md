# Day 3: REST API Layer - Completed! ✅

## What Was Built

### 1. DTOs (Data Transfer Objects)
- ✅ Created `CreateOrderRequest` - Request DTO for creating orders
- ✅ Created `OrderResponse` - Response DTO with complete order data
- ✅ Created `OrderItemResponse` - Nested DTO for order items
- ✅ Created `AddressDto` - Address information DTO
- ✅ Created `MoneyDto` - Money/currency DTO
- ✅ Added Jakarta Bean Validation annotations (@NotNull, @NotBlank, @Valid)

### 2. DTO Mapping Layer
- ✅ Created `OrderDtoMapper` - Converts between DTOs and domain models
- ✅ Implemented bidirectional mapping (DTO ↔ Domain)
- ✅ Maintains separation between API contracts and domain logic

### 3. Application Service Layer
- ✅ Created `OrderService` - Orchestrates business operations
- ✅ Implemented transaction management with @Transactional
- ✅ Added CRUD operations and state transition methods
- ✅ Created `OrderNotFoundException` for error handling

### 4. REST API Controller
- ✅ Created `OrderController` with 8 endpoints
- ✅ Implemented full CRUD operations
- ✅ Added state transition endpoints (pay, ship, cancel)
- ✅ Proper HTTP status codes (201 Created, 404 Not Found, 400 Bad Request)
- ✅ Added OpenAPI/Swagger annotations for documentation

### 5. Global Exception Handling
- ✅ Created `GlobalExceptionHandler` with @RestControllerAdvice
- ✅ Handles validation errors (400 Bad Request)
- ✅ Handles business rule violations (400 Bad Request)
- ✅ Handles not found errors (404 Not Found)
- ✅ Handles unexpected errors (500 Internal Server Error)
- ✅ Consistent error response format across all endpoints

### 6. API Documentation
- ✅ Added Springdoc OpenAPI dependency
- ✅ Created `OpenApiConfig` configuration
- ✅ Auto-generated Swagger UI at `/swagger-ui.html`
- ✅ Auto-generated OpenAPI spec at `/v3/api-docs`

### 7. Integration Tests
- ✅ Created `OrderControllerIntegrationTest` with 12 comprehensive tests
- ✅ Tests use MockMvc for HTTP layer testing
- ✅ Tests use Testcontainers for real database
- ✅ Tests verify full stack: Controller → Service → Repository → Database
- ✅ Tests validate JSON serialization/deserialization
- ✅ Tests verify HTTP status codes and error responses

## Architecture Layers (Top to Bottom)

```
┌─────────────────────────────────────────────────────────┐
│ REST API Layer (Inbound Adapter)                        │
│ - OrderController: HTTP endpoints                       │
│ - GlobalExceptionHandler: Error responses               │
│ - DTOs: CreateOrderRequest, OrderResponse               │
│ - OrderDtoMapper: DTO ↔ Domain conversion              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Application Layer (Use Cases)                           │
│ - OrderService: Orchestrates operations                 │
│ - Transaction management                                 │
│ - Exception handling                                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Domain Layer (Business Logic)                           │
│ - Order: Aggregate root                                 │
│ - OrderItem, Money, Address: Value objects              │
│ - OrderRepository: Port interface                       │
│ - Business rules enforcement                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Persistence Layer (Outbound Adapter)                    │
│ - OrderRepositoryAdapter: Port implementation           │
│ - JpaOrderRepository: Spring Data JPA                   │
│ - OrderEntity, OrderItemEntity: JPA entities            │
│ - PostgreSQL database                                    │
└─────────────────────────────────────────────────────────┘
```

## API Endpoints

### Order Management

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| POST | `/api/orders` | Create new order | 201, 400 |
| GET | `/api/orders/{orderId}` | Get order by ID | 200, 404 |
| GET | `/api/orders?customerId={id}` | Get orders by customer | 200 |
| GET | `/api/orders/all` | Get all orders | 200 |

### Order Operations

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| POST | `/api/orders/{orderId}/pay` | Mark order as paid | 200, 400, 404 |
| POST | `/api/orders/{orderId}/ship` | Mark order as shipped | 200, 400, 404 |
| POST | `/api/orders/{orderId}/cancel` | Cancel order | 200, 400, 404 |

## Sample API Requests

### Create Order
```json
POST /api/orders
{
  "customerId": "CUST-123",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": {
        "amount": 99.99,
        "currency": "USD"
      },
      "quantity": 1
    }
  ]
}
```

### Response (201 Created)
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-123",
  "status": "CREATED",
  "createdAt": "2025-12-24T10:30:00Z",
  "paidAt": null,
  "shippedAt": null,
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": {
        "amount": 99.99,
        "currency": "USD"
      },
      "quantity": 1,
      "lineTotal": {
        "amount": 99.99,
        "currency": "USD"
      }
    }
  ],
  "totalAmount": {
    "amount": 99.99,
    "currency": "USD"
  }
}
```

### Error Response (400 Bad Request)
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "customerId": "Customer ID is required",
    "items": "Order must have at least one item"
  },
  "timestamp": "2025-12-24T10:30:00Z"
}
```

## How to Run

### Prerequisites
- Java 17+
- Maven 3.6+ (or use IDE's Maven integration)
- Docker & Docker Compose

### Start Database
```bash
cd order-fulfillment-system
docker-compose up -d
```

### Run Application
```bash
# Using Maven
mvn spring-boot:run

# Or using IDE: Run OrderFulfillmentApplication.main()
```

### Access API Documentation
Once the application is running:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **API Base URL**: http://localhost:8080/api/orders

### Run Tests
```bash
# Run all tests
mvn clean test

# Run only REST API tests
mvn test -Dtest=OrderControllerIntegrationTest

# Run all integration tests
mvn test -Dtest=*IntegrationTest
```

### Test with cURL
```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "US"
    },
    "items": [{
      "productId": "PROD-001",
      "productName": "Product 1",
      "unitPrice": {"amount": 99.99, "currency": "USD"},
      "quantity": 1
    }]
  }'

# Get order by ID (replace ORDER_ID)
curl http://localhost:8080/api/orders/{ORDER_ID}

# Mark order as paid
curl -X POST http://localhost:8080/api/orders/{ORDER_ID}/pay

# Mark order as shipped
curl -X POST http://localhost:8080/api/orders/{ORDER_ID}/ship
```

## Hexagonal Architecture Implementation

### Why DTOs Separate from Domain?

**Benefits:**
1. **API Stability**: Change domain without breaking API contracts
2. **Security**: Control exactly what data is exposed/accepted
3. **Validation**: Add API-specific validation rules (different from domain rules)
4. **Flexibility**: Support multiple API versions with same domain

**Example:**
```java
// Domain Model - Business-focused
public class Order {
    private String orderId;
    private OrderStatus status;  // Enum
    private Money total;         // Value object
    // ... business methods
}

// API DTO - Client-focused
public record OrderResponse(
    String orderId,
    String status,           // String for JSON
    MoneyDto totalAmount     // Simple DTO
) {}
```

### Service Layer Responsibilities

**What belongs in OrderService:**
- ✅ Transaction management (@Transactional)
- ✅ Orchestrating multiple domain operations
- ✅ Calling repository methods
- ✅ Exception translation

**What does NOT belong in OrderService:**
- ❌ Business rules (those go in Order domain model)
- ❌ HTTP concerns (those go in OrderController)
- ❌ Validation logic (use @Valid in controller + domain rules)
- ❌ DTO mapping (use OrderDtoMapper)

### Global Exception Handler Benefits

**Without @RestControllerAdvice:**
```java
@PostMapping
public ResponseEntity<?> createOrder() {
    try {
        // happy path
    } catch (ValidationException e) {
        return ResponseEntity.badRequest()...
    } catch (NotFoundException e) {
        return ResponseEntity.notFound()...
    } catch (Exception e) {
        return ResponseEntity.internalServerError()...
    }
}
```

**With @RestControllerAdvice:**
```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder() {
    // Only happy path - exceptions handled globally!
    return ResponseEntity.created()...
}
```

## Code Review Discussion Points

### ✅ RESTful API Design
**Question:** "Why use POST for operations like pay/ship/cancel instead of PUT?"
**Answer:** These are commands that trigger state transitions, not resource updates. POST is appropriate for actions. PUT is for replacing entire resources with idempotent updates.

### ✅ DTO Pattern
**Question:** "Isn't it duplication to have both Order and OrderResponse?"
**Answer:** They serve different purposes:
- `Order`: Domain model with business logic
- `OrderResponse`: API contract for JSON serialization
This separation allows them to evolve independently.

### ✅ MockMvc vs RestTemplate
**Question:** "Why use MockMvc instead of RestTemplate in tests?"
**Answer:** 
- MockMvc: Faster (no HTTP server), tests Spring MVC layer
- RestTemplate/TestRestTemplate: Slower (starts server), tests full HTTP stack
For most cases, MockMvc is sufficient.

### ✅ Transaction Boundaries
**Question:** "Why @Transactional on service methods, not on controller methods?"
**Answer:** 
- Services define transaction boundaries (use case boundaries)
- Controllers handle HTTP concerns (outside transaction scope)
- Following Single Responsibility Principle

### ⚠️ Pagination Missing
**Note:** The `GET /api/orders/all` endpoint could return thousands of orders. In production:
- Add pagination with `Pageable` parameter
- Return `Page<OrderResponse>` instead of `List<OrderResponse>`
- Spring Data JPA makes this easy!

## Lessons Learned

### Spring Boot Magic
- Auto-configures web server (Tomcat embedded)
- Auto-configures Jackson for JSON serialization
- Auto-configures exception handling with @RestControllerAdvice
- Minimal configuration needed

### Validation Strategy
- **API Layer**: @Valid, @NotNull, @NotBlank (syntactic validation)
- **Domain Layer**: Business rule validation in methods (semantic validation)
- **Example**: API validates "price must be positive", Domain validates "order total must be > $0"

### Testing Strategy
- **Unit Tests**: Test domain logic in isolation (Day 1)
- **Integration Tests (Repository)**: Test persistence layer (Day 2)
- **Integration Tests (REST API)**: Test full stack end-to-end (Day 3)
- Each layer builds confidence

### OpenAPI/Swagger Benefits
- Interactive API documentation
- Client SDK generation
- Contract-first API development
- API versioning support

## Next Steps (Day 4)

Tomorrow we could add:
- [ ] Event publishing (domain events for PAID, SHIPPED, CANCELLED)
- [ ] Message queue integration (send events to RabbitMQ/Kafka)
- [ ] Email notifications on order state changes
- [ ] Advanced queries (search orders by date range, status)
- [ ] Pagination and sorting for list endpoints
- [ ] API versioning (/api/v1/orders, /api/v2/orders)
- [ ] Security (Spring Security with JWT)
- [ ] Rate limiting
- [ ] Caching with Redis

## Interview Talking Points

> "I built a RESTful API using Spring Boot with hexagonal architecture. The controller layer handles HTTP concerns, DTOs provide API contracts separate from the domain, and a service layer orchestrates business operations with proper transaction management."

> "I used @RestControllerAdvice for centralized exception handling, which provides consistent error responses across all endpoints and keeps controllers focused on the happy path. This follows the Single Responsibility Principle."

> "For testing, I wrote integration tests using MockMvc and Testcontainers to verify the entire stack from HTTP request through to database persistence. These tests caught several issues that unit tests alone wouldn't have found."

> "The DTO pattern allows the API contract to evolve independently of the domain model. For example, we can change the Order aggregate's internal structure without breaking client applications, as long as we maintain the DTO mapping."

> "I documented the API using Springdoc OpenAPI, which auto-generates Swagger UI from code annotations. This provides interactive documentation and can generate client SDKs in multiple languages."

## File Structure After Day 3

```
src/main/java/com/midlevel/orderfulfillment/
├── OrderFulfillmentApplication.java
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── OrderController.java           ← NEW
│   │       ├── GlobalExceptionHandler.java    ← NEW
│   │       ├── dto/
│   │       │   ├── CreateOrderRequest.java    ← NEW
│   │       │   ├── OrderResponse.java         ← NEW
│   │       │   ├── AddressDto.java            ← NEW
│   │       │   └── MoneyDto.java              ← NEW
│   │       └── mapper/
│   │           └── OrderDtoMapper.java        ← NEW
│   └── out/
│       └── persistence/
│           ├── OrderRepositoryAdapter.java
│           ├── JpaOrderRepository.java
│           ├── entity/...
├── application/
│   └── OrderService.java                      ← NEW
├── config/
│   └── OpenApiConfig.java                     ← NEW
└── domain/
    ├── model/
    │   ├── Order.java
    │   ├── OrderItem.java
    │   ├── Money.java
    │   ├── Address.java
    │   └── OrderStatus.java
    └── port/
        └── OrderRepository.java

src/test/java/com/midlevel/orderfulfillment/
├── adapter/
│   ├── in/
│   │   └── web/
│   │       └── OrderControllerIntegrationTest.java  ← NEW
│   └── out/
│       └── persistence/
│           └── OrderRepositoryIntegrationTest.java
└── domain/
    └── model/
        └── OrderTest.java
```

---

**Day 3 Status:** ✅ Complete  
**Deliverable:** Full REST API with 8 endpoints, global error handling, OpenAPI docs, and 12 passing integration tests  
**Time Invested:** 3-4 hours  
**Commits:** Ready for code review  
**Total Tests:** 25+ (Unit + Repository + REST API Integration)
