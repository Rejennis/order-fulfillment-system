# Data Flow Through the Order Fulfillment System

This system uses **Hexagonal Architecture** with **Event-Driven patterns**. Here's how data flows through it:

## 🔄 Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT (HTTP Request)                         │
│                POST /api/orders                                  │
│                { customerId, items, address }                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  INBOUND ADAPTER: OrderController (REST API)                    │
│  - Receives HTTP request                                        │
│  - Validates DTO (@Valid)                                       │
│  - Maps DTO → Domain Model                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  APPLICATION LAYER: OrderService                                │
│  - Opens transaction (@Transactional)                           │
│  - Calls domain logic                                           │
│  - Coordinates persistence                                      │
│  - Publishes events AFTER commit                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  DOMAIN LAYER: Order Aggregate                                  │
│  - Enforces business rules                                      │
│  - Manages state transitions                                    │
│  - Registers domain events (OrderCreatedEvent)                  │
│  - Returns updated aggregate                                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  DOMAIN PORT: OrderRepository Interface                         │
│  - save(order)                                                  │
│  - Abstraction for persistence                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  OUTBOUND ADAPTER: OrderRepositoryAdapter                       │
│  - Converts Domain → JPA Entity                                 │
│  - Calls jpaOrderRepository.save()                              │
│  - Converts JPA Entity → Domain                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  DATABASE: PostgreSQL                                            │
│  - Persists OrderEntity                                          │
│  - Transaction commits                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  APPLICATION LAYER: DomainEventPublisher                        │
│  - Extracts domain events from Order                            │
│  - Publishes to Spring ApplicationEventPublisher                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┴─────────────────────┐
        ↓                                           ↓
┌──────────────────────┐              ┌──────────────────────────┐
│  EVENT LISTENER      │              │  NOTIFICATION SERVICE    │
│  OrderEventListener  │              │  NotificationService     │
│  @Async              │              │  @Async                  │
│  - Logs event        │→─────────────→│  - Sends emails         │
│  - Warehouse notify  │              │  - SMS/Push (future)    │
└──────────────────────┘              └──────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│  EXTERNAL SYSTEMS                                                │
│  - Email Service (notifications)                                │
│  - Warehouse System (inventory/shipping)                        │
│  - Analytics (event tracking)                                   │
└─────────────────────────────────────────────────────────────────┘
```

## 📋 Step-by-Step Flow for "Create Order"

### 1. HTTP Request → OrderController
**File:** `src/main/java/com/midlevel/orderfulfillment/adapter/in/web/OrderController.java`

- Client sends JSON: `POST /api/orders`
- DTO validation with `@Valid`
- Maps `CreateOrderRequest` → `Order` domain model

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    Order order = mapper.toDomain(request);
    Order savedOrder = orderService.createOrder(order);
    OrderResponse response = mapper.toResponse(savedOrder);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### 2. Application Service → OrderService.createOrder()
**File:** `src/main/java/com/midlevel/orderfulfillment/application/OrderService.java`

- Opens database transaction
- Calls `orderRepository.save(order)`
- Calls `eventPublisher.publishEvents(order)`
- Returns saved order

```java
@Transactional
public Order createOrder(Order order) {
    Order savedOrder = orderRepository.save(order);
    eventPublisher.publishEvents(savedOrder);
    return savedOrder;
}
```

### 3. Domain Model → Order.create()
**File:** `src/main/java/com/midlevel/orderfulfillment/domain/model/Order.java`

- Validates business rules
- Sets status to `CREATED`
- Registers `OrderCreatedEvent`
- Maintains aggregate consistency

```java
public static Order create(String customerId, List<OrderItem> items, Address shippingAddress) {
    String orderId = UUID.randomUUID().toString();
    // Validation logic...
    Order order = new Order(orderId, customerId, items, shippingAddress);
    order.registerEvent(new OrderCreatedEvent(orderId, customerId, totalAmount, items.size()));
    return order;
}
```

### 4. Repository Port → OrderRepository Interface
**File:** `src/main/java/com/midlevel/orderfulfillment/domain/port/OrderRepository.java`

- Domain-defined abstraction
- No implementation details
- Dependency inversion principle

```java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
    // ... other methods
}
```

### 5. Repository Adapter → OrderRepositoryAdapter
**File:** `src/main/java/com/midlevel/orderfulfillment/adapter/out/persistence/OrderRepositoryAdapter.java`

- Converts `Order` → `OrderEntity` (JPA)
- Saves to PostgreSQL via Spring Data JPA
- Converts back to domain model

```java
@Override
public Order save(Order order) {
    OrderEntity entity = OrderEntity.fromDomain(order);
    OrderEntity savedEntity = jpaOrderRepository.save(entity);
    return savedEntity.toDomain();
}
```

### 6. Event Publishing → DomainEventPublisher
**File:** `src/main/java/com/midlevel/orderfulfillment/application/DomainEventPublisher.java`

- **After transaction commits** ✅
- Publishes events to Spring's event bus
- Non-blocking, asynchronous

```java
public void publish(DomainEvent event) {
    applicationEventPublisher.publishEvent(event);
}
```

### 7. Event Handling → OrderEventListener
**File:** `src/main/java/com/midlevel/orderfulfillment/adapter/out/event/OrderEventListener.java`

- Runs **async in separate thread** (@Async)
- Sends notifications via NotificationService
- Logs events
- Triggers warehouse operations

```java
@EventListener
@Async
public void handleOrderCreated(OrderCreatedEvent event) {
    log.info("📦 Order Created: {} for customer {}", event.getOrderId(), event.getCustomerId());
    orderRepository.findById(event.getOrderId()).ifPresent(order -> {
        notificationService.notifyOrderCreated(order);
    });
    reserveInventory(event);
}
```

## 🔀 State Transition Flow

```
CREATED ──pay()──→ PAID ──ship()──→ SHIPPED
   │                  │
   │                  │
   └──cancel()──→ CANCELLED ←──cancel()──┘
```

Each state transition:
- Enforces preconditions in domain
- Registers appropriate domain event
- Updates aggregate state
- Persists changes
- Publishes events asynchronously

### State Transition Details

| Transition | Method | Precondition | Event | Business Rule |
|------------|--------|--------------|-------|---------------|
| → CREATED | `create()` | None | `OrderCreatedEvent` | Must have items, total > 0 |
| CREATED → PAID | `pay()` | Status = CREATED | `OrderPaidEvent` | Idempotent operation |
| PAID → SHIPPED | `ship()` | Status = PAID | `OrderShippedEvent` | Cannot ship unpaid orders |
| CREATED/PAID → CANCELLED | `cancel()` | Status ≠ SHIPPED | `OrderCancelledEvent` | Cannot cancel shipped |

## 🎯 Key Architectural Patterns

### 1. Hexagonal Architecture (Ports & Adapters)
- **Domain** is framework-independent
- **Adapters** translate between layers
- **Dependencies point inward** (toward domain)
- **Ports** are interfaces defined in domain

**Structure:**
```
adapter.in.web (OrderController)
    → application (OrderService)
        → domain (Order, OrderRepository interface)
            ← adapter.out.persistence (OrderRepositoryAdapter)
```

### 2. Event-Driven Architecture
- **Domain events** decouple reactions from actions
- **Async processing** improves performance
- **Multiple listeners** can react to same event
- **Eventual consistency** for non-critical operations

**Benefits:**
- Notifications don't block order processing
- Easy to add new reactions without modifying existing code
- System remains responsive under load

### 3. CQRS-lite
- **Commands**: `createOrder()`, `markOrderAsPaid()`, `markOrderAsShipped()`
- **Queries**: `findById()`, `findByCustomerId()`, `findByStatus()`
- **Transaction boundaries** are clear
- Read operations are read-only for performance

### 4. Domain-Driven Design (DDD)
- **Order** is Aggregate Root
- **Rich domain model** with business logic
- **Ubiquitous language** in code
- **Value Objects**: Money, OrderItem, Address
- **Entities**: Order
- **Ports**: Repository interfaces

## 🔐 Transaction Boundaries

The transaction lifecycle is critical for data consistency:

1. **Transaction opens**: `@Transactional` annotation on OrderService method
2. **Domain logic executes**: Order validates and changes state
3. **Database write**: Order persisted via repository
4. **Transaction commits**: Changes become permanent in database
5. **Events publish**: **After successful commit** (important!)
6. **Async handlers**: Run outside transaction scope

### Why Events Publish After Commit?

```java
@Transactional
public Order createOrder(Order order) {
    Order savedOrder = orderRepository.save(order);  // Inside transaction
    eventPublisher.publishEvents(savedOrder);         // After commit
    return savedOrder;
}
```

**Reasoning:**
- If transaction fails, events are not published
- Prevents inconsistent state (event published but data not saved)
- Follows transactional outbox pattern principles

## 📊 Data Transformation Flow

### Inbound Flow (Client → Domain)
```
JSON Request (DTO)
    → CreateOrderRequest
    → OrderDtoMapper.toDomain()
    → Order (domain model)
```

### Outbound Flow (Domain → Database)
```
Order (domain model)
    → OrderEntity.fromDomain()
    → OrderEntity (JPA)
    → PostgreSQL tables
```

### Response Flow (Domain → Client)
```
Order (domain model)
    → OrderDtoMapper.toResponse()
    → OrderResponse (DTO)
    → JSON Response
```

## 🎭 Complete Example: Pay for an Order

### Request
```http
POST /api/orders/{orderId}/pay
```

### Flow
```
1. OrderController.markOrderAsPaid(orderId)
   └→ 2. OrderService.markOrderAsPaid(orderId)
       ├→ 3. orderRepository.findById(orderId)
       │   └→ OrderRepositoryAdapter → JPA → Database
       ├→ 4. order.pay() [Domain validates: must be CREATED]
       │   └→ Registers OrderPaidEvent
       ├→ 5. orderRepository.save(order)
       │   └→ OrderRepositoryAdapter → JPA → Database
       └→ 6. eventPublisher.publishEvents(order)
           └→ 7. OrderEventListener.handleOrderPaid(event) [@Async]
               ├→ NotificationService.notifyOrderPaid(order)
               │   └→ Send payment confirmation email
               └→ notifyWarehouse(event)
                   └→ Warehouse prepares shipment
```

### Response
```json
{
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PAID",
  "paidAt": "2025-12-25T10:30:00Z",
  "totalAmount": 99.99,
  "currency": "USD"
}
```

## 🚀 Performance Considerations

### Async Event Processing
- Events processed in separate thread pool
- Main request returns immediately
- Notification failures don't affect order processing

### Transaction Optimization
- Read operations use `@Transactional(readOnly = true)`
- Write operations override with `@Transactional`
- Reduces database connection overhead

### Idempotency
- `markOrderAsPaid()` is idempotent
- Multiple calls with same orderId don't cause errors
- Important for payment webhooks and network retries

## 🧪 Testing Strategy

### Unit Tests (Domain Layer)
- Test Order aggregate in isolation
- No Spring, no database
- Fast execution (milliseconds)

### Integration Tests (Application Layer)
- Test with real database (Testcontainers)
- Test event publishing
- Verify transactions work correctly

### API Tests (Adapter Layer)
- Test HTTP endpoints
- Verify DTO mapping
- Check HTTP status codes and responses

## 📝 Summary

The data flows through well-defined layers with clear responsibilities:

1. **Adapters** handle external concerns (HTTP, database)
2. **Application** orchestrates workflows and transactions
3. **Domain** contains business logic and rules
4. **Events** enable loose coupling and async processing

This architecture ensures:
- ✅ Business logic is isolated and testable
- ✅ Technology can be swapped without affecting domain
- ✅ System is responsive and scalable
- ✅ Code is maintainable and follows SOLID principles
