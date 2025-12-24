# Day 2: Repository & Persistence Layer - Completed! ✅

## What Was Built

### 1. Spring Boot Integration
- ✅ Migrated to Spring Boot 3.2.1 with parent POM
- ✅ Added Spring Data JPA for database access
- ✅ Configured PostgreSQL driver
- ✅ Added Testcontainers for integration testing

### 2. Database Configuration
- ✅ Created `docker-compose.yml` with PostgreSQL 16
- ✅ Configured `application.yml` with database settings
- ✅ Added `application-test.yml` for test configuration
- ✅ Set up connection pooling with HikariCP

### 3. JPA Entity Layer
- ✅ Created `OrderEntity` - JPA entity for orders table
- ✅ Created `OrderItemEntity` - JPA entity for order_items table
- ✅ Created `AddressEmbeddable` - Embedded address value object
- ✅ Implemented bidirectional mapping domain ↔ entity

### 4. Repository Pattern (Hexagonal Architecture)
- ✅ Defined `OrderRepository` port interface in domain layer
- ✅ Created `JpaOrderRepository` extending Spring Data JPA
- ✅ Implemented `OrderRepositoryAdapter` bridging port to JPA
- ✅ Demonstrated dependency inversion (domain doesn't depend on JPA)

### 5. Integration Tests with Testcontainers
- ✅ Created 12 comprehensive integration tests
- ✅ Tests use real PostgreSQL database in Docker
- ✅ Verified CRUD operations
- ✅ Tested custom queries
- ✅ Validated entity mappings and cascades
- ✅ Tested complete order lifecycle persistence

## Architecture Highlights

### Hexagonal Architecture (Ports & Adapters)
```
Domain Layer (Core)
├── Order (Aggregate Root)
├── OrderRepository (Port Interface)  ← Defined by domain
└── Business Logic

Adapter Layer (Infrastructure)
├── OrderEntity (JPA Entity)
├── OrderRepositoryAdapter (Port Implementation)
└── JpaOrderRepository (Spring Data JPA)
```

**Key Principle:** Domain defines what it needs (port), infrastructure provides it (adapter).

### Why Separate Domain from JPA?

**Benefits:**
1. **Clean Domain**: No JPA annotations polluting business logic
2. **Testability**: Can test domain with fake repositories
3. **Flexibility**: Can swap PostgreSQL for MongoDB without touching domain
4. **Independence**: Domain evolves separately from infrastructure

**Trade-off:**
- More classes (OrderEntity + Order)
- Mapping between domain and entities

**When to use:** Mid-to-large projects where domain complexity justifies the separation.

## Database Schema

The following tables are auto-created by Hibernate:

```sql
-- orders table
CREATE TABLE orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    shipped_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(2) NOT NULL
);

-- order_items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

## How to Run

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Start Database
```bash
# Start PostgreSQL in Docker
cd order-fulfillment-system
docker-compose up -d

# Verify it's running
docker ps
```

### Run Tests
```bash
# Run all tests (includes unit + integration tests)
mvn clean test

# Run only integration tests
mvn test -Dtest=OrderRepositoryIntegrationTest

# Run with coverage
mvn clean test jacoco:report
```

### Run Application
```bash
# Run Spring Boot application
mvn spring-boot:run

# Application starts on http://localhost:8080
```

### Access Database
**Via psql:**
```bash
docker exec -it orderfulfillment-postgres psql -U postgres -d orderfulfillment
```

**Via PgAdmin:**
- Open http://localhost:5050
- Login: admin@orderfulfillment.com / admin
- Add server: postgres / postgres / postgres

## Code Review Discussion Points

### ✅ Domain Isolation
**Question:** "Why not just use JPA entities directly?"
**Answer:** Domain logic should be independent of infrastructure. If we change from JPA to MongoDB, only the adapter changes - domain stays the same.

### ✅ Repository Pattern
**Question:** "Why both OrderRepository and JpaOrderRepository?"
**Answer:** 
- `OrderRepository` (port): What the domain needs
- `JpaOrderRepository`: How Spring Data JPA provides it
- `OrderRepositoryAdapter`: Translator between them

### ✅ Testcontainers
**Question:** "Why not just mock the repository in tests?"
**Answer:** Integration tests verify the full stack works. Mocking hides bugs in:
- JPA mappings
- SQL queries
- Transaction handling
- Database constraints

### ⚠️ Entity Reconstitution
**Note:** Current implementation uses reflection to restore domain state from entities. In production:
- Add package-private constructor in Order
- Or use factory method for reconstitution
- Reflection is a smell but acceptable for learning

## Lessons Learned

### Spring Data JPA Magic
- No need to write SQL for common queries
- Method naming conventions generate queries
- `@Query` for complex cases
- Automatic transaction management

### Testcontainers Benefits
- Real database in tests (not H2/in-memory)
- Catches JPA mapping issues early
- CI/CD friendly (if Docker available)
- Test database cleanup automatic

### Connection Pooling
- HikariCP is default in Spring Boot
- Configured in `application.yml`
- Prevents connection exhaustion
- Improves performance

## Next Steps (Day 3)

Tomorrow we'll build on this foundation:
- [ ] Create REST API controllers
- [ ] Implement DTOs for API requests/responses
- [ ] Add service layer for business logic orchestration
- [ ] Document API with OpenAPI/Swagger

## Interview Talking Points

> "I implemented a persistence layer using Spring Data JPA with a hexagonal architecture pattern. The domain layer defines what it needs through a repository port interface, and the infrastructure layer provides the implementation using JPA entities and Spring Data repositories. This keeps the domain isolated from infrastructure concerns."

> "I wrote integration tests using Testcontainers, which spins up a real PostgreSQL database in Docker for testing. This caught several JPA mapping issues that unit tests with mocks would have missed, like incorrect cascade configurations and entity relationship mappings."

> "The trade-off of separating domain models from JPA entities is more code, but it provides better testability, flexibility to change persistence mechanisms, and keeps the domain clean from infrastructure concerns."

---

**Day 2 Status:** ✅ Complete  
**Deliverable:** Working persistence layer with 12 passing integration tests  
**Time Invested:** 2-3 hours  
**Commits:** Ready for code review
