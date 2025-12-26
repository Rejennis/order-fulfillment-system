# How to Run Integration Tests

## Why This Document Exists

This guide focuses on **Day 9 integration tests** because that's when comprehensive **Testcontainers-based integration testing** was introduced. Day 9 added Kafka event streaming with full end-to-end testing that requires Docker.

**Previous days** had simpler unit tests that could run with just `mvn test` - no special setup needed.

**Day 9** introduced:
- Testcontainers (requires Docker)
- Kafka integration tests
- End-to-end workflow tests
- ~13 integration tests that spin up real PostgreSQL and Kafka containers

---

## Prerequisites

1. **Docker Desktop Running**
   - Testcontainers requires Docker
   - Verify: `docker ps` should work

2. **Maven Installed**
   - Check: `mvn --version`
   - Should show Maven 3.6+ and Java 17+

## Running Tests

### All Tests (Including Day 9 Kafka Tests)

```bash
cd order-fulfillment-system
mvn test
```

### Only Kafka Integration Tests

```bash
# Both Kafka test classes
mvn test -Dtest=Kafka*IntegrationTest

# Individual test class
mvn test -Dtest=KafkaEventIntegrationTest
mvn test -Dtest=KafkaEndToEndIntegrationTest
```

### From IDE (IntelliJ/Eclipse/VS Code)

1. Right-click on test class
2. Select "Run 'KafkaEventIntegrationTest'"
3. Wait for Testcontainers to download images (first time only)

## What to Expect

### First Run (Slow - One Time Only)
```
[Testcontainers] Pulling docker image: postgres:16
[Testcontainers] Pulling docker image: confluentinc/cp-kafka:7.5.0
⏱️ Time: 2-3 minutes (image download)
```

### Subsequent Runs (Fast)
```
[Testcontainers] Using cached image: postgres:16
[Testcontainers] Using cached image: confluentinc/cp-kafka:7.5.0
⏱️ Time: 30-45 seconds (container startup)
```

### Expected Output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.midlevel.orderfulfillment.adapter.kafka.KafkaEventIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 25.3 s
[INFO] 
[INFO] Running com.midlevel.orderfulfillment.adapter.kafka.KafkaEndToEndIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 32.1 s
[INFO]
[INFO] Results:
[INFO] 
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Test Classes Overview

### KafkaEventIntegrationTest (6 tests)
Tests Kafka-specific functionality:
- Event publishing to topics
- Event consumption by listeners
- Idempotency handling
- Error resilience
- Event ordering per partition

### KafkaEndToEndIntegrationTest (7 tests)
Tests complete workflows:
- REST API → Database → Kafka → Notifications
- Create, pay, ship, cancel flows
- Concurrent order processing
- Idempotency with duplicate events

## Troubleshooting

### "Could not find a valid Docker environment"
**Solution:** Start Docker Desktop
```bash
# Verify Docker is running
docker ps
```

### "Port already in use"
**Solution:** Testcontainers uses random ports, shouldn't conflict
- Stop any running order-fulfillment-system app
- Check: `netstat -ano | findstr "8080"`

### Tests timeout
**Solution:** Increase wait time (rarely needed)
- First run: images downloading (patient)
- Subsequent runs: containers starting (15-30 sec)
- Check Docker has enough resources (4GB RAM minimum)

### Maven not found
**Option 1: Install Maven**
- Download: https://maven.apache.org/download.cgi
- Add to PATH

**Option 2: Use IDE's built-in Maven**
- IntelliJ: Uses bundled Maven
- VS Code: Install "Java Extension Pack"
- Eclipse: Uses embedded Maven

**Option 3: Maven Wrapper (if available)**
```bash
# Windows
mvnw.cmd test

# Linux/Mac
./mvnw test
```

## Continuous Integration

These tests are designed to run in CI/CD:

### GitHub Actions Example
```yaml
- name: Run Tests
  run: mvn test
  env:
    TESTCONTAINERS_RYUK_DISABLED: false
```

### Docker-in-Docker
- Testcontainers supports DinD
- Works with GitLab CI, Jenkins, CircleCI

## Performance Tips

### Speed up tests
```bash
# Skip unit tests, run only integration
mvn verify -DskipUnitTests

# Run tests in parallel
mvn test -T 1C  # 1 thread per CPU core
```

### Clean build
```bash
mvn clean test  # Fresh compilation + tests
```

### Skip tests temporarily
```bash
mvn install -DskipTests  # Build without testing
```

## What Gets Tested

### Day 9 Integration Tests (13 tests)

✅ **Kafka Integration Tests (6 tests):**
- Event publishing to topics
- Event consumption by listeners
- Idempotency handling
- Error resilience
- Event ordering per partition

✅ **End-to-End Workflow Tests (7 tests):**
- REST API → Database → Kafka → Notifications
- Create, pay, ship, cancel flows
- Concurrent order processing
- Idempotency with duplicate events

### Day 10 Observability
- Unit tests for metrics configuration
- Health indicator tests
- No additional Docker requirements

### Day 11 Resilience
- Retry pattern (tested via existing tests with failure injection)
- Circuit breaker (tested via existing tests with Kafka unavailability)
- Exception handler (tested via REST API tests)
- Future: ResilienceIntegrationTest.java

### Earlier Days (Unit Tests)
- Domain model validation (Order, OrderItem, Money)
- Service layer logic
- Repository tests with H2 in-memory database
- **No Docker required** - simple `mvn test` works

---

## Summary

**RUN_TESTS.md exists specifically for Day 9** because that's when integration testing became complex enough to require documentation. 

**Key differences by day:**
- **Days 1-8**: Simple unit tests, no Docker needed
- **Day 9**: Integration tests with Testcontainers + Docker required ← This document
- **Days 10-11**: Enhanced observability and resilience, tested via Day 9's infrastructure

**To run everything:** `mvn test` (with Docker running for Day 9 tests)

**To skip integration tests:** `mvn test -DskipTests=*IntegrationTest`

---

**Summary:** Run `mvn test` with Docker running to verify Day 9+ completion.
