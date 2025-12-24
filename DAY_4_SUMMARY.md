what happened to day_3_# Day 4: Domain Events & Event-Driven Architecture - Completed! ✅

## What Was Built

### 1. Domain Event Classes
- ✅ Created `DomainEvent` - Base class for all domain events
- ✅ Created `OrderCreatedEvent` - Published when order is created
- ✅ Created `OrderPaidEvent` - Published when order is paid
- ✅ Created `OrderShippedEvent` - Published when order is shipped
- ✅ Created `OrderCancelledEvent` - Published when order is cancelled
- ✅ Each event carries relevant business data

### 2. Event Registration in Domain Model
- ✅ Modified `Order` aggregate to collect domain events
- ✅ Added `registerEvent()` method to collect events
- ✅ Added `getDomainEvents()` to expose events
- ✅ Added `clearDomainEvents()` to clear after publishing
- ✅ Events registered during state transitions (create, pay, ship, cancel)

### 3. Event Publishing Infrastructure
- ✅ Created `DomainEventPublisher` - Bridges domain to Spring events
- ✅ Modified `OrderService` to publish events after transactions
- ✅ Events published after successful database commits
- ✅ Events cleared after publishing to prevent duplicates

### 4. Event Listeners
- ✅ Created `OrderEventListener` with handlers for all events
- ✅ Implemented async event processing with @Async
- ✅ Added logging for visibility
- ✅ Simulated side effects (emails, inventory, notifications)

### 5. Async Configuration
- ✅ Created `AsyncConfig` with @EnableAsync
- ✅ Event listeners run in separate threads
- ✅ Main transaction doesn't wait for event processing

### 6. Comprehensive Tests
- ✅ Created `OrderDomainEventsTest` - Unit tests for event registration
- ✅ Created `OrderServiceEventPublishingTest` - Integration tests for full event flow
- ✅ Tests verify events are published and received correctly
- ✅ Tests use CountDownLatch for async verification

## Architecture: Event-Driven Design

### Event Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Domain Layer - Event Registration                        │
│                                                              │
│   Order.pay() {                                             │
│       this.status = PAID;                                    │
│       registerEvent(new OrderPaidEvent(...)); // Collect    │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Application Layer - Event Publishing                     │
│                                                              │
│   OrderService.markOrderAsPaid() {                          │
│       order.pay();                                           │
│       orderRepository.save(order); // Commit DB             │
│       eventPublisher.publishAll(order.getDomainEvents());   │
│       order.clearDomainEvents();                            │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Infrastructure - Spring Application Events               │
│                                                              │
│   DomainEventPublisher {                                    │
│       applicationEventPublisher.publishEvent(event);        │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Event Listeners - Side Effects (Async)                  │
│                                                              │
│   @EventListener @Async                                     │
│   void handleOrderPaid(OrderPaidEvent event) {             │
│       sendPaymentReceipt(event);                            │
│       notifyWarehouse(event);                               │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
```

### Domain Events Pattern (DDD)

**What are Domain Events?**
> Domain Events represent something significant that happened in the domain that domain experts care about.

**Key Characteristics:**
1. **Named in Past Tense**: `OrderCreated`, not `CreateOrder` (they're facts)
2. **Immutable**: Events represent things that already happened
3. **Contain Business Data**: Enough info for listeners to react
4. **Part of Ubiquitous Language**: Domain experts understand "Order was paid"

**Benefits:**
1. **Decoupling**: Producers don't know about consumers
2. **Extensibility**: Add new reactions without changing existing code
3. **Audit Trail**: Events provide history of what happened
4. **Eventual Consistency**: Allow async processing
5. **Integration**: Easy to publish to external systems

### Why Separate Event Registration from Publishing?

**Domain Layer (Registration):**
```java
// Order.java - Domain Model
public void pay() {
    this.status = PAID;
    registerEvent(new OrderPaidEvent(...));  // Collect, don't publish
}
```

**Application Layer (Publishing):**
```java
// OrderService.java - Application Service
public Order markOrderAsPaid(String orderId) {
    order.pay();
    orderRepository.save(order);
    eventPublisher.publishAll(order.getDomainEvents());  // Publish after commit
    order.clearDomainEvents();
}
```

**Why?**
1. **Domain Independence**: Order doesn't depend on Spring
2. **Transaction Safety**: Events published AFTER database commit
3. **Testability**: Can test domain without event infrastructure
4. **Flexibility**: Can publish to different systems (Kafka, RabbitMQ, etc.)

## Event Types and Their Purpose

### OrderCreatedEvent
**When**: Order is created
**Contains**: orderId, customerId, totalAmount, itemCount
**Triggers**:
- Send order confirmation email
- Reserve inventory
- Notify analytics system
- Create audit log entry

### OrderPaidEvent
**When**: Payment is received
**Contains**: orderId, customerId, totalAmount, paidAt
**Triggers**:
- Send payment receipt
- Authorize warehouse to ship
- Update financial records
- Calculate sales commissions

### OrderShippedEvent
**When**: Order leaves warehouse
**Contains**: orderId, customerId, shippedAt
**Triggers**:
- Send tracking notification
- Deduct final inventory
- Update expected delivery date
- Notify customer service

### OrderCancelledEvent
**When**: Order is cancelled
**Contains**: orderId, customerId, reason
**Triggers**:
- Send cancellation notification
- Release reserved inventory
- Process refund if paid
- Update analytics

## Async Event Processing

### Why @Async?

**Without Async:**
```
API Request → Create Order → Save to DB → Send Email → Return Response
                                         ↑ Blocks here (slow!)
Total time: 500ms (DB) + 2000ms (email) = 2500ms
```

**With Async:**
```
API Request → Create Order → Save to DB → Publish Event → Return Response
                                              ↓ (async)
                                         Send Email
Total time: 500ms (email happens in background)
```

**Benefits:**
1. **Fast Response Times**: API returns immediately
2. **Better UX**: User doesn't wait for side effects
3. **Resilience**: Email failure doesn't fail order creation
4. **Scalability**: Event processing can be distributed

**Tradeoffs:**
1. **Eventual Consistency**: Email sent slightly after response
2. **Error Handling**: Need to handle async failures
3. **Testing Complexity**: Must wait for async operations in tests

## Code Review Discussion Points

### ✅ Domain Event Registration Pattern
**Question:** "Why collect events in Order instead of publishing directly?"
**Answer:** 
- Domain should be infrastructure-agnostic
- Events published after transaction commits (not during state changes)
- Can test domain logic without event infrastructure
- Follows DDD principles

### ✅ Event Sourcing vs. Domain Events
**Question:** "Is this Event Sourcing?"
**Answer:** No, different patterns:
- **Event Sourcing**: Events ARE the source of truth (no tables, rebuild state from events)
- **Domain Events**: Events are notifications about state changes (tables still exist)
- We're using Domain Events (simpler, more common)

### ✅ Transactional Boundaries
**Question:** "What if event publishing fails?"
**Answer:** 
- DB transaction already committed before events published
- Order is saved even if event publishing fails
- In production: Use transactional outbox pattern or retry logic
- Trade-off: Simpler code vs. guaranteed event delivery

### ✅ Multiple Event Listeners
**Question:** "Can multiple listeners react to the same event?"
**Answer:** Yes! That's the power of events:
```java
@EventListener
void handleOrderPaid(OrderPaidEvent event) { sendEmail(...); }

@EventListener
void handleOrderPaid(OrderPaidEvent event) { updateAnalytics(...); }

@EventListener
void handleOrderPaid(OrderPaidEvent event) { notifyWarehouse(...); }
```
All three listeners react to the same event independently.

### ⚠️ Guaranteed Delivery
**Note:** Current implementation doesn't guarantee event delivery if:
- Application crashes after DB commit but before event publishing
- Event listener throws exception

**Production Solutions:**
1. **Transactional Outbox Pattern**: Store events in DB, publish later
2. **Message Queue**: Publish to Kafka/RabbitMQ with acknowledgments
3. **Spring Retry**: Auto-retry failed event processing

## Lessons Learned

### Spring Application Events
- Built into Spring Framework (no extra dependencies)
- Synchronous by default, async with @Async
- Type-safe (compile-time checking)
- Easy to test with test event listeners

### Event-Driven Benefits
- **Maintainability**: Add new features without modifying existing code
- **Scalability**: Listeners can run on different servers
- **Observability**: Events provide natural audit log
- **Integration**: Easy to connect external systems

### Testing Async Events
- Use `CountDownLatch` to wait for async operations
- Test event listeners separately from domain
- Integration tests verify full event flow
- Unit tests verify event registration

## Next Steps (Day 5)

Tomorrow we could add:
- [ ] Pagination and sorting for list endpoints
- [ ] Advanced search/filtering (by status, date range, amount)
- [ ] API versioning (/api/v1, /api/v2)
- [ ] Security with Spring Security
- [ ] JWT authentication
- [ ] Rate limiting
- [ ] Redis caching for frequent queries
- [ ] Metrics and monitoring (Micrometer)
- [ ] External message queue (Kafka/RabbitMQ)
- [ ] Transactional outbox pattern

## Interview Talking Points

> "I implemented domain events following DDD principles. The Order aggregate registers events when state changes occur, and the application service publishes them after the database transaction commits. This ensures consistency and allows multiple listeners to react independently."

> "Events are processed asynchronously using Spring's @Async, which improves API response times. For example, sending confirmation emails doesn't block the order creation response. The main transaction completes first, then side effects happen in the background."

> "I used Spring Application Events for simplicity, but the architecture is flexible. The DomainEventPublisher abstracts the publishing mechanism, so we could easily swap to Kafka or RabbitMQ without changing the domain layer."

> "The event-driven pattern provides excellent extensibility. To add new behavior like updating analytics when orders are paid, I just add a new event listener - no changes to existing code. This follows the Open-Closed Principle."

> "For testing, I created both unit tests to verify event registration in the domain and integration tests to verify the full flow from service to listeners. I used CountDownLatch to properly wait for asynchronous event processing in tests."

## File Structure After Day 4

```
src/main/java/com/midlevel/orderfulfillment/
├── OrderFulfillmentApplication.java
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── OrderController.java
│   │       ├── GlobalExceptionHandler.java
│   │       ├── dto/...
│   │       └── mapper/...
│   └── out/
│       ├── event/
│       │   └── OrderEventListener.java           ← NEW
│       └── persistence/
│           ├── OrderRepositoryAdapter.java
│           ├── JpaOrderRepository.java
│           └── entity/...
├── application/
│   ├── OrderService.java                         ← UPDATED
│   └── DomainEventPublisher.java                 ← NEW
├── config/
│   ├── OpenApiConfig.java
│   └── AsyncConfig.java                          ← NEW
└── domain/
    ├── event/                                     ← NEW
    │   ├── DomainEvent.java                      ← NEW
    │   ├── OrderCreatedEvent.java                ← NEW
    │   ├── OrderPaidEvent.java                   ← NEW
    │   ├── OrderShippedEvent.java                ← NEW
    │   └── OrderCancelledEvent.java              ← NEW
    ├── model/
    │   ├── Order.java                            ← UPDATED
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
│   │       └── OrderControllerIntegrationTest.java
│   └── out/
│       └── persistence/
│           └── OrderRepositoryIntegrationTest.java
├── application/
│   └── OrderServiceEventPublishingTest.java      ← NEW
├── domain/
│   ├── event/
│   │   └── OrderDomainEventsTest.java            ← NEW
│   └── model/
│       └── OrderTest.java
```

---

**Day 4 Status:** ✅ Complete  
**Deliverable:** Event-driven architecture with domain events, async processing, and comprehensive tests  
**Time Invested:** 2-3 hours  
**Commits:** Ready for code review  
**Total Tests:** 35+ (Unit + Repository + REST API + Events)

## Key Takeaways

1. **Domain Events are powerful**: They provide loose coupling and extensibility
2. **Async processing improves UX**: Users don't wait for side effects
3. **Separation is important**: Domain registers, infrastructure publishes
4. **Multiple listeners can react**: Each listener is independent
5. **Testing async code requires patience**: Use CountDownLatch and timeouts

## Real-World Applications

**E-Commerce:**
- OrderPaidEvent → Send receipt, authorize shipping
- OrderShippedEvent → Send tracking, deduct inventory
- OrderCancelledEvent → Process refund, release stock

**Banking:**
- TransactionCompletedEvent → Update balance, send SMS
- AccountCreatedEvent → Send welcome email, create card
- LowBalanceEvent → Send alert, suggest credit line

**Healthcare:**
- AppointmentScheduledEvent → Send reminder, notify doctor
- PrescriptionFilledEvent → Update records, bill insurance
- LabResultsReadyEvent → Notify patient, alert physician

Domain events are a fundamental pattern in modern microservices and event-driven architectures!
