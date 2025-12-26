# How to Run Day 9 Integration Tests

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

✅ **Tested:**
- Kafka event publishing
- Kafka event consumption
- REST API integration
- Database persistence
- Notification triggering
- Idempotency
- Error handling

❌ **Not Tested:**
- Real email sending (mocked)
- Kafka cluster failover (single broker)
- Network partitions (requires special setup)

---

**Summary:** Run `mvn test` with Docker running to verify Day 9 completion.
