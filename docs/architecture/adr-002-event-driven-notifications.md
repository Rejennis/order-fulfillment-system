# ADR-002: Event-Driven Architecture with Domain Events

**Status:** Accepted  
**Date:** December 24, 2025  
**Decision Makers:** Development Team  
**Context Date:** Day 4 Implementation (Actually Day 7 in mentor program)

---

## Context

When an order changes state (created, paid, shipped, cancelled), other parts of the system need to react:
- Send email notifications to customers
- Notify warehouse to prepare shipment
- Update analytics dashboards
- Trigger external integrations (CRM, accounting)
- Maintain audit logs

**Problem:** How do we decouple these reactions from the core order processing logic?

## Decision

We will use **Domain Events** with Spring's `ApplicationEventPublisher` to implement an event-driven architecture.

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Domain Layer (Event Registration)                      │
│                                                          │
│ Order.create() → registerEvent(OrderCreatedEvent)      │
│ Order.pay()    → registerEvent(OrderPaidEvent)         │
│ Order.ship()   → registerEvent(OrderShippedEvent)      │
│ Order.cancel() → registerEvent(OrderCancelledEvent)    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Application Layer (Event Publishing)                    │
│                                                          │
│ OrderService.createOrder()                              │
│   ├── Save order to database                            │
│   ├── Transaction commits                               │
│   └── DomainEventPublisher.publishEvents(order)        │
│       └── Publishes to Spring ApplicationEventPublisher│
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Adapter Layer (Event Consumers)                         │
│                                                          │
│ @EventListener @Async                                   │
│ OrderEventListener.handleOrderCreated()                 │
│   └── Send "Order Confirmed" email                      │
│                                                          │
│ @EventListener @Async                                   │
│ OrderEventListener.handleOrderPaid()                    │
│   └── Notify warehouse for preparation                  │
│                                                          │
│ @EventListener @Async                                   │
│ OrderEventListener.handleOrderShipped()                 │
│   └── Send "Order Shipped" email with tracking         │
└─────────────────────────────────────────────────────────┘
```

## Event Classes

### Base Event
```java
public abstract class DomainEvent {
    private final Instant occurredAt;
    
    // Immutable events - represent facts
    // Named in past tense (what happened)
}
```

### Concrete Events
```java
public class OrderCreatedEvent extends DomainEvent {
    private final String orderId;
    private final String customerId;
    private final Money totalAmount;
    private final int itemCount;
}

public class OrderPaidEvent extends DomainEvent {
    private final String orderId;
    private final String customerId;
    private final Money totalAmount;
    private final Instant paidAt;
}

// OrderShippedEvent, OrderCancelledEvent...
```

## Implementation Pattern

### 1. Domain registers events (doesn't publish)
```java
public class Order {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    public static Order create(...) {
        Order order = new Order(...);
        order.registerEvent(new OrderCreatedEvent(...));
        return order;
    }
    
    public void pay() {
        // Business logic
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
        
        // Register event
        registerEvent(new OrderPaidEvent(...));
    }
}
```

### 2. Service publishes after transaction
```java
@Service
public class OrderService {
    @Transactional
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        
        // Events published AFTER DB commit succeeds
        eventPublisher.publishEvents(savedOrder);
        
        return savedOrder;
    }
}
```

### 3. Listeners react asynchronously
```java
@Component
public class OrderEventListener {
    @EventListener
    @Async  // Runs in separate thread
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Sending order confirmation email...");
        // emailService.sendOrderConfirmation(event);
    }
}
```

## Consequences

### Positive

✅ **Decoupling**
- Order aggregate doesn't know about emails, notifications, or analytics
- Can add new listeners without modifying Order class
- Single Responsibility Principle - each component has one job

✅ **Scalability**
- Events processed asynchronously (doesn't block order creation)
- Can move to message queue (Kafka) later without changing domain
- Multiple listeners can react to same event independently

✅ **Auditability**
- Events provide complete audit trail
- Can replay events to rebuild state
- Foundation for Event Sourcing if needed later

✅ **Resilience**
- Main transaction succeeds even if listener fails
- Can implement retry logic per listener
- Failed notifications don't block order processing

✅ **Testability**
- Can test event registration in domain without infrastructure
- Can test listeners in isolation
- Can verify events were published in integration tests

### Negative

⚠️ **Eventual Consistency**
- Email might arrive seconds after order is created
- Customer might see order before receiving confirmation
- Need to handle "notification sent but email failed" scenarios

⚠️ **Debugging Complexity**
- Harder to trace async flows
- Need correlation IDs and structured logging
- "Why didn't customer get email?" requires event debugging

⚠️ **Event Ordering**
- No guarantee OrderPaidEvent arrives before OrderShippedEvent
- Listeners must be idempotent (can handle duplicate events)
- Need to handle out-of-order events gracefully

⚠️ **Transaction Boundaries**
- Events published after DB commit (not atomic)
- If event publishing fails, DB is already committed
- Need Outbox pattern for transactional guarantees (future enhancement)

### Mitigation Strategies

**For Eventual Consistency:**
- Set customer expectations ("Confirmation email within 5 minutes")
- Show in-app notifications immediately
- Log all event publishing attempts

**For Debugging:**
- Add correlation IDs to all events
- Use structured logging (JSON format)
- Implement distributed tracing (future: Jaeger/Zipkin)

**For Event Ordering:**
- Include timestamp in events
- Make listeners idempotent (check if already processed)
- Version events if schema changes

**For Transaction Boundaries:**
- Accept that occasional email might fail (retry later)
- Future: Implement Outbox pattern for guaranteed delivery
- Monitor failed event publishing and alert

## Alternatives Considered

### Alternative 1: Direct Service Calls
```java
@Service
public class OrderService {
    private final EmailService emailService;
    private final WarehouseService warehouseService;
    
    public Order createOrder(Order order) {
        Order saved = repository.save(order);
        emailService.sendConfirmation(saved);  // Tight coupling!
        warehouseService.notifyNewOrder(saved);
        return saved;
    }
}
```

**Why not chosen:**
- Tight coupling between services
- Hard to add new behaviors
- Slow (all operations sequential)
- Failures cascade (email failure blocks order)

### Alternative 2: Message Queue (Kafka) from Day 1
```java
kafkaProducer.send("order-created", orderEvent);
```

**Why not chosen (for now):**
- Over-engineering for initial implementation
- Spring Events sufficient for single-service deployment
- Can migrate to Kafka later without changing domain
- YAGNI (You Aren't Gonna Need It... yet)

**When to use Kafka:**
- Multiple microservices need events
- Need guaranteed delivery with persistence
- Events cross service boundaries
- High throughput requirements (>1000 orders/sec)

### Alternative 3: Observer Pattern (Classic OOP)
```java
public class Order {
    private List<OrderObserver> observers;
    
    public void notifyObservers() {
        observers.forEach(o -> o.onOrderCreated(this));
    }
}
```

**Why not chosen:**
- Domain would depend on observer interfaces (violation of hexagonal)
- Hard to make async
- Doesn't integrate well with Spring
- Domain Events are better fit for DDD

## Evolution Path

### Phase 1: Spring Events (Current)
✅ Good for single deployment
✅ Simple to implement
✅ No external dependencies

### Phase 2: Kafka Integration (Day 9)
- Keep Spring Events for internal listeners
- Add Kafka producer as additional listener
- External systems consume from Kafka
- Enables microservices architecture

### Phase 3: Event Sourcing (Future)
- Store events as source of truth
- Rebuild order state from events
- Complete audit trail
- Time travel queries

## Configuration

### Enable Async Processing
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    // Uses Spring's default ThreadPoolTaskExecutor
}
```

### Event Publisher Bridge
```java
@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher publisher;
    
    public void publishEvents(Order order) {
        order.getDomainEvents().forEach(publisher::publishEvent);
        order.clearDomainEvents();
    }
}
```

## Testing Strategy

### Unit Tests (Domain)
```java
@Test
void orderCreationShouldRegisterEvent() {
    Order order = Order.create(...);
    
    List<DomainEvent> events = order.getDomainEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(OrderCreatedEvent.class);
}
```

### Integration Tests (Event Publishing)
```java
@Test
void shouldPublishEventAfterOrderCreation() {
    CountDownLatch latch = new CountDownLatch(1);
    
    Order order = orderService.createOrder(...);
    
    latch.await(5, TimeUnit.SECONDS);
    // Verify event was received by listener
}
```

## Related Decisions

- [ADR-001: Hexagonal Architecture](adr-001-hexagonal-architecture.md)
- [ADR-003: JPA for Persistence](adr-003-jpa-for-persistence.md)

## References

- [Domain Events - Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Spring Events Documentation](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Implementing Domain-Driven Design - Vaughn Vernon](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)

## Review History

| Date | Reviewer | Decision |
|------|----------|----------|
| 2025-12-24 | Development Team | Accepted |

---

**Last Updated:** December 24, 2025  
**Status:** ✅ Implemented with Spring Events  
**Next Evolution:** Kafka integration (Day 9)
