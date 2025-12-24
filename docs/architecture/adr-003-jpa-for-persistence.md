# ADR-003: JPA with PostgreSQL for Persistence

**Status:** Accepted  
**Date:** December 24, 2025  
**Decision Makers:** Development Team  
**Context Date:** Day 2 Implementation

---

## Context

Our Order Fulfillment System needs to persist orders reliably with:
- ACID transactions (can't lose orders or have partial saves)
- Complex queries (find by customer, filter by status, date ranges)
- Relational integrity (order → order items relationship)
- Support for future features (analytics, reporting)

We need to choose a persistence technology that balances:
- Developer productivity
- Performance
- Reliability
- Team expertise

## Decision

We will use **JPA (Java Persistence API)** with **Hibernate** as the implementation and **PostgreSQL** as the database.

### Technology Stack

```
┌─────────────────────────────────────────────────────────┐
│ Domain Layer                                            │
│ - Order (domain model - pure Java, no JPA)            │
│ - OrderRepository (port interface)                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Adapter Layer - Persistence                             │
│                                                          │
│ OrderRepositoryAdapter (implements OrderRepository)     │
│   ├── Converts: Domain Order ↔ JPA OrderEntity        │
│   └── Delegates to: JpaOrderRepository                 │
│                                                          │
│ JpaOrderRepository (extends JpaRepository)              │
│   └── Spring Data JPA magic methods                     │
│                                                          │
│ OrderEntity, OrderItemEntity (JPA annotations)          │
│   └── @Entity, @Table, @OneToMany, etc.               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ PostgreSQL Database                                      │
│ - orders table                                           │
│ - order_items table (with FK to orders)                 │
└─────────────────────────────────────────────────────────┘
```

## Architecture Pattern: Separate Domain from Persistence

### Domain Model (Clean, Framework-Free)
```java
public class Order {
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private OrderStatus status;
    
    // Business logic, no JPA annotations
    public void pay() { ... }
    public void ship() { ... }
}
```

### JPA Entity (Infrastructure Concern)
```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String orderId;
    
    private String customerId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}
```

### Adapter Maps Between Them
```java
@Component
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
}
```

## Consequences

### Positive

✅ **Domain Protection**
- Domain model stays clean (no `@Entity`, `@Table`, etc.)
- Can change persistence without touching business logic
- True hexagonal architecture

✅ **Developer Productivity**
- JPA handles SQL generation automatically
- Spring Data JPA provides repositories with zero code
- Boilerplate CRUD operations for free
- Query methods from method names (`findByCustomerId`)

✅ **ACID Transactions**
- PostgreSQL ensures atomic commits
- `@Transactional` manages transaction boundaries
- Rollback on exceptions
- Consistent view of data

✅ **Type Safety**
- Compile-time query validation (better than raw SQL strings)
- Entity relationships mapped in Java types
- IDE autocompletion and refactoring support

✅ **Lazy Loading & Performance**
- Fetch associations only when needed
- Can optimize with `@EntityGraph` later
- Batch fetching support

✅ **Migration Path**
- JPA is vendor-neutral (can switch from Hibernate to EclipseLink)
- Can switch from PostgreSQL to MySQL/Oracle with minimal changes
- Standards-based (JPA spec)

### Negative

⚠️ **N+1 Query Problem**
- Default lazy loading can cause performance issues
- Need to be careful with associations
- Requires monitoring and tuning

⚠️ **Mapping Overhead**
- Need to convert: Domain ↔ Entity
- More code to maintain
- Slight runtime cost (usually negligible)

⚠️ **Learning Curve**
- JPA has complex behavior (persistence context, proxies)
- Hibernate can feel "magical"
- Team needs to understand lifecycle

⚠️ **Abstraction Leak**
- Entity relationships affect queries
- Cascade operations can be tricky
- Lazy initialization exceptions if not careful

### Mitigation Strategies

**For N+1 Problems:**
- Use `JOIN FETCH` in custom queries
- Enable query logging in dev: `spring.jpa.show-sql=true`
- Add query performance tests
- Future: Add database query monitoring (Hibernate statistics)

**For Mapping Overhead:**
- Use library like MapStruct for automatic mapping
- Test mapping layer independently
- Keep mapping logic simple and testable

**For Learning Curve:**
- Code reviews check for JPA anti-patterns
- Document common pitfalls (lazy init exceptions)
- Use integration tests to catch issues early

## Configuration

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderfulfillment
    username: orderuser
    password: orderpass
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-create in production!
    show-sql: false       # Enable in dev for debugging
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### docker-compose.yml
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: orderfulfillment
      POSTGRES_USER: orderuser
      POSTGRES_PASSWORD: orderpass
    ports:
      - "5432:5432"
```

## Schema Design

### orders table
```sql
CREATE TABLE orders (
    order_id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    shipped_at TIMESTAMP,
    
    -- Address embedded in same table (denormalized for simplicity)
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(50),
    shipping_zip_code VARCHAR(20),
    shipping_country VARCHAR(100),
    
    -- Indexes
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### order_items table
```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price_amount DECIMAL(19, 4) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);
```

## Testing Strategy

### Integration Tests with Testcontainers
```java
@SpringBootTest
@Testcontainers
class OrderRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        // ... configure username, password
    }
    
    @Test
    void shouldSaveAndRetrieveOrder() {
        // Test against real PostgreSQL
    }
}
```

**Why Testcontainers:**
- Tests run against real database (catches mapping issues)
- No need for mocks or H2 (in-memory DB)
- Tests are reproducible
- CI/CD can run same tests

## Alternatives Considered

### Alternative 1: Spring Data JDBC
```java
@Table("orders")
public class Order {
    @Id String orderId;
    // Simpler than JPA, no lazy loading magic
}
```

**Why not chosen:**
- Less feature-rich than JPA
- No lazy loading (always eager fetch)
- Less community support
- Team has more JPA experience

**When to reconsider:** If JPA complexity becomes problematic

### Alternative 2: jOOQ (SQL DSL)
```java
dsl.selectFrom(ORDERS)
   .where(ORDERS.CUSTOMER_ID.eq(customerId))
   .fetch();
```

**Why not chosen:**
- More SQL-focused (less object-oriented)
- Requires code generation from schema
- Steeper learning curve
- Less abstraction (more control, more code)

**When to reconsider:** If we need complex SQL queries, reporting

### Alternative 3: MyBatis (SQL Mapper)
```java
@Select("SELECT * FROM orders WHERE customer_id = #{customerId}")
List<Order> findByCustomerId(String customerId);
```

**Why not chosen:**
- Need to write SQL manually
- Less type-safe than JPA
- More boilerplate
- Team prefers JPA abstractions

### Alternative 4: NoSQL (MongoDB)
```json
{ "orderId": "123", "items": [ ... ] }
```

**Why not chosen:**
- Orders have clear relational structure (order → items)
- Need ACID transactions
- PostgreSQL JSON support gives us flexibility anyway
- Relational queries (find by customer) are natural

**When to reconsider:** If we need extreme horizontal scaling

## Migration Strategy

### Phase 1: JPA with Hibernate (Current)
✅ Rapid development
✅ Standard approach
✅ Good for up to millions of orders

### Phase 2: Query Optimization (Future)
- Add database indexes based on actual query patterns
- Use `@EntityGraph` for complex fetches
- Add read replicas if needed
- Cache frequently accessed orders

### Phase 3: CQRS (Future, if needed)
- Separate read model (optimized views)
- Keep JPA for writes
- Event-driven synchronization
- Only if query performance becomes issue

## Performance Considerations

### Current Scale
- Expected: < 1000 orders/day initially
- JPA + PostgreSQL easily handles this

### Future Scale Limits
- JPA good up to ~10,000 orders/minute
- PostgreSQL good up to ~millions of orders total
- If we exceed this: consider CQRS, sharding, or NoSQL for reads

### Monitoring Plan
- Track slow queries (> 100ms)
- Monitor connection pool usage
- Set up query performance tests in CI
- Add database health checks

## Related Decisions

- [ADR-001: Hexagonal Architecture](adr-001-hexagonal-architecture.md) - Why we separate domain from entities
- [ADR-002: Event-Driven Architecture](adr-002-event-driven-notifications.md) - Events complement persistence

## References

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Best Practices](https://wiki.postgresql.org/wiki/Don%27t_Do_This)
- [Testcontainers](https://www.testcontainers.org/)
- [Hibernate Performance Tuning](https://vladmihalcea.com/tutorials/hibernate/)

## Review History

| Date | Reviewer | Decision |
|------|----------|----------|
| 2025-12-24 | Development Team | Accepted |

---

**Last Updated:** December 24, 2025  
**Status:** ✅ Implemented  
**Database:** PostgreSQL 16 in Docker  
**Test Strategy:** Testcontainers for real database testing
