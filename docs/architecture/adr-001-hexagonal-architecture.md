# ADR-001: Hexagonal Architecture (Ports & Adapters)

**Status:** Accepted  
**Date:** December 24, 2025  
**Decision Makers:** Development Team  
**Context Date:** Day 3 Implementation

---

## Context

We need to architect our Order Fulfillment System in a way that:
- Separates business logic from infrastructure concerns
- Makes the system testable without external dependencies
- Allows adapters (database, API, messaging) to be swapped easily
- Protects the domain model from framework pollution

## Decision

We will use **Hexagonal Architecture** (also known as Ports & Adapters pattern) to structure our application.

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│ Inbound Adapters (Driving Side)                        │
│ - OrderController (REST API)                           │
│ - Future: CLI, GraphQL, gRPC                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Application Layer (Use Cases)                           │
│ - OrderService (orchestration)                          │
│ - Transaction boundaries                                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Domain Layer (Business Logic) - THE CORE                │
│ - Order aggregate root                                  │
│ - Value objects (Money, OrderItem, Address)             │
│ - Port interfaces (OrderRepository)                     │
│ - Domain events                                          │
│ - Business rules enforcement                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Outbound Adapters (Driven Side)                        │
│ - OrderRepositoryAdapter (JPA/PostgreSQL)              │
│ - OrderEventListener (Event handling)                  │
│ - Future: Kafka producer, Email service                │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.midlevel.orderfulfillment/
├── domain/                    # Core business logic (framework-free)
│   ├── model/                 # Aggregates & value objects
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Money.java
│   │   └── Address.java
│   ├── event/                 # Domain events
│   └── port/                  # Port interfaces (abstractions)
│       └── OrderRepository.java
│
├── application/               # Use cases & orchestration
│   ├── OrderService.java
│   └── DomainEventPublisher.java
│
├── adapter/                   # Infrastructure implementations
│   ├── in/                    # Inbound (driving) adapters
│   │   └── web/
│   │       ├── OrderController.java
│   │       ├── dto/
│   │       └── mapper/
│   └── out/                   # Outbound (driven) adapters
│       ├── persistence/
│       │   ├── OrderRepositoryAdapter.java
│       │   └── entity/
│       └── event/
│           └── OrderEventListener.java
│
└── config/                    # Spring configuration
```

## Consequences

### Positive

✅ **Testability**
- Domain logic can be tested without Spring, database, or HTTP
- Unit tests run in milliseconds
- Integration tests use real implementations via adapters

✅ **Framework Independence**
- Domain doesn't depend on Spring, JPA, or any framework
- Can switch from Spring to Micronaut/Quarkus without touching domain
- Business logic survives technology changes

✅ **Flexibility**
- Can swap PostgreSQL for MongoDB by implementing OrderRepository differently
- Can add GraphQL API alongside REST without touching domain
- Can add Kafka alongside Spring Events

✅ **Clear Boundaries**
- Dependencies flow inward (adapters → application → domain)
- Domain has no outward dependencies
- Easy to see where business rules live (always in domain)

✅ **Team Scalability**
- Domain experts can work on domain/ without knowing Spring
- Infrastructure devs can work on adapters/ without touching business rules
- Clear separation of concerns

### Negative

⚠️ **More Code**
- Need mapper classes (DTO ↔ Domain, Entity ↔ Domain)
- More files to maintain
- Can feel like "over-engineering" for simple CRUD

⚠️ **Learning Curve**
- Team needs to understand hexagonal concepts
- Junior devs might be confused by indirection
- Requires discipline to maintain boundaries

⚠️ **Mapping Overhead**
- Performance cost of converting between layers (usually negligible)
- Boilerplate mapping code

### Mitigation

- Use MapStruct for automatic mapping generation
- Document architecture clearly (this ADR!)
- Code reviews enforce boundary rules
- Benefits outweigh costs as system grows

## Alternatives Considered

### Alternative 1: Layered Architecture (Traditional N-Tier)
```
Controller → Service → Repository → Database
```

**Why not chosen:**
- Business logic tends to leak into services
- Strong coupling to frameworks (hard to test)
- Difficult to swap implementations
- No clear domain model

### Alternative 2: Anemic Domain Model
```
POJOs + Service Layer with all logic
```

**Why not chosen:**
- Violates OOP principles (data + behavior together)
- Business rules scattered across services
- Hard to maintain as complexity grows
- Not aligned with DDD principles

### Alternative 3: Clean Architecture (Uncle Bob)
Very similar to Hexagonal, mainly terminology differences.

**Why Hexagonal over Clean:**
- More established pattern for Java/Spring
- Better community examples
- Clearer "port" terminology for abstractions

## Implementation Notes

### Dependency Rule
**Critical:** Dependencies point INWARD only.

```java
✅ GOOD: Controller → Service → Domain
✅ GOOD: Domain defines OrderRepository port
✅ GOOD: Adapter implements OrderRepository port

❌ BAD: Domain → Spring annotations
❌ BAD: Domain → JPA entities
❌ BAD: Domain → HTTP concerns
```

### Port Examples

**Primary Port (Inbound):**
```java
// Application service - entry point for use cases
@Service
public class OrderService {
    public Order createOrder(Order order) { ... }
}
```

**Secondary Port (Outbound):**
```java
// Domain defines what it needs
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
}
```

**Adapter implements port:**
```java
// Infrastructure provides implementation
@Component
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    // Maps domain Order ↔ JPA OrderEntity
}
```

## Related Decisions

- [ADR-002: Event-Driven Architecture](adr-002-event-driven-notifications.md)
- [ADR-003: JPA for Persistence](adr-003-jpa-for-persistence.md)

## References

- [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [DDD Distilled - Vaughn Vernon](https://www.amazon.com/Domain-Driven-Design-Distilled-Vaughn-Vernon/dp/0134434420)

## Review History

| Date | Reviewer | Decision |
|------|----------|----------|
| 2025-12-24 | Development Team | Accepted |

---

**Last Updated:** December 24, 2025  
**Status:** ✅ Implemented across entire codebase
