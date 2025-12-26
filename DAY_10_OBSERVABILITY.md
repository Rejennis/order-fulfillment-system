# Day 10: Observability & Monitoring - In Progress

## 🎯 Objectives

Add production-ready observability to the order fulfillment system:
- ✅ Structured JSON logging
- ✅ Correlation IDs for request tracing
- ✅ Custom business metrics with Micrometer
- ✅ Health checks (Kafka, Database)
- ✅ Actuator endpoints
- ⏳ Enhanced logging in all layers
- ⏳ Testing and validation

---

## ✅ Completed Tasks

### 1. Dependencies Added (pom.xml)

**Spring Boot Actuator**
- Production-ready features (health, metrics, info)
- Exposes /actuator/* endpoints

**Micrometer Prometheus Registry**
- Exports metrics in Prometheus format
- Available at /actuator/prometheus

**Logback Logstash Encoder**
- JSON-formatted structured logging
- Machine-readable logs for aggregation

### 2. Configuration (application.yml)

**Actuator Endpoints Exposed:**
- `/actuator/health` - Application health status
- `/actuator/info` - Application metadata
- `/actuator/metrics` - All metrics
- `/actuator/prometheus` - Prometheus-format metrics
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Log level management
- `/actuator/threaddump` - Thread dump for debugging
- `/actuator/heapdump` - Heap dump for memory analysis

**Health Check Configuration:**
- Show detailed health information
- Include database, disk space checks
- Custom Kafka health indicator

**Metrics Configuration:**
- Application tag added to all metrics
- HTTP request histograms with percentiles
- SLA buckets: 50ms, 100ms, 200ms, 500ms, 1s, 2s

### 3. Structured Logging (logback-spring.xml)

**Development Profile (default):**
- Human-readable console logs
- Includes correlation ID in pattern
- DEBUG level for application code

**Production Profile (prod):**
- JSON-formatted logs via Logstash encoder
- Includes MDC context (correlation IDs)
- File appender with 30-day rotation
- INFO level for application code

**Log Format Example:**
```json
{
  "@timestamp": "2025-12-26T15:00:00.123+00:00",
  "message": "Order created successfully",
  "logger": "com.midlevel.orderfulfillment.application.OrderService",
  "thread": "http-nio-8080-exec-1",
  "level": "INFO",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "app": "order-fulfillment-system",
  "orderId": "order-123",
  "customerId": "customer-456"
}
```

### 4. Correlation ID Filter

**CorrelationIdFilter.java**
- Intercepts every HTTP request
- Extracts or generates correlation ID
- Adds to MDC (Mapped Diagnostic Context)
- Includes in response header `X-Correlation-Id`
- Enables end-to-end request tracing

**Benefits:**
- Trace single request across all logs
- Debug production issues efficiently
- Correlate frontend → backend → database → Kafka
- Required for distributed tracing

### 5. Custom Business Metrics

**MetricsConfiguration.java**

Created 8 custom counters and timers:

1. **orders.created.total** - Counter
   - Tracks total orders created
   - Tagged with: domain=order-fulfillment

2. **orders.failures.total** - Counter
   - Tracks failed order operations
   - Critical for alerting

3. **orders.status.changes.total** - Counter
   - Tracks state transitions
   - Can be tagged by status (PAID, SHIPPED, etc.)

4. **orders.creation.duration** - Timer
   - Measures order creation latency
   - Includes count, sum, max, percentiles

5. **events.published.total** - Counter
   - Tracks Kafka events published
   - Monitors event-driven architecture health

6. **events.consumed.total** - Counter
   - Tracks Kafka events consumed
   - Detects consumer lag

7. **events.failures.total** - Counter
   - Tracks event processing failures
   - Critical for DLQ monitoring

8. **notifications.sent.total** - Counter
   - Tracks notification delivery
   - Monitors notification system health

### 6. Custom Health Indicators

**KafkaHealthIndicator.java**
- Checks Kafka cluster connectivity
- Reports cluster ID and node count
- Status: UP if reachable, DOWN if not
- Contributes to `/actuator/health`

**Built-in Health Indicators (Auto-configured):**
- Database connectivity (PostgreSQL)
- Disk space threshold (10MB minimum)
- Application liveness/readiness

### 7. Enhanced OrderService

**Added Observability:**
- Structured logging for all operations
- Metrics recording (counters, timers)
- Correlation IDs in log context
- Success/failure tracking
- Performance measurements

**Logging Examples:**
```
INFO  - Creating order for customer: customer-123, items: 3
INFO  - Order created successfully: orderId=order-456, customerId=customer-123, totalAmount=99.99
INFO  - Marking order as paid: orderId=order-456
INFO  - Order marked as paid successfully: orderId=order-456, previousStatus=CREATED
ERROR - Failed to mark order as paid: orderId=order-999
```

---

## 📊 How to Use Observability Features

### View Application Health

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-123",
        "nodeCount": 1,
        "status": "Kafka cluster is reachable"
      }
    }
  }
}
```

### View All Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

**Key Metrics:**
- `jvm.memory.used` - JVM memory usage
- `jvm.threads.live` - Active threads
- `http.server.requests` - HTTP request metrics
- `orders.created.total` - Custom order metric
- `kafka.producer.record.send.total` - Kafka producer stats

### View Specific Metric

```bash
curl http://localhost:8080/actuator/metrics/orders.created.total
```

**Response:**
```json
{
  "name": "orders.created.total",
  "description": "Total number of orders created",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42.0
    }
  ],
  "availableTags": [
    {
      "tag": "domain",
      "values": ["order-fulfillment"]
    }
  ]
}
```

### Prometheus Metrics (for Scraping)

```bash
curl http://localhost:8080/actuator/prometheus
```

**Sample Output:**
```
# HELP orders_created_total Total number of orders created
# TYPE orders_created_total counter
orders_created_total{application="order-fulfillment-system",domain="order-fulfillment",} 42.0

# HELP orders_creation_duration_seconds Time taken to create orders
# TYPE orders_creation_duration_seconds summary
orders_creation_duration_seconds_count{application="order-fulfillment-system",domain="order-fulfillment",} 42.0
orders_creation_duration_seconds_sum{application="order-fulfillment-system",domain="order-fulfillment",} 2.5
orders_creation_duration_seconds_max{application="order-fulfillment-system",domain="order-fulfillment",} 0.150
```

### Application Info

```bash
curl http://localhost:8080/actuator/info
```

**Response:**
```json
{
  "app": {
    "name": "order-fulfillment-system",
    "description": "Order Fulfillment System - Mid-Level Java Training Project",
    "version": "1.0-SNAPSHOT",
    "java": {
      "version": "17"
    }
  }
}
```

### Change Log Level at Runtime

```bash
# Set specific logger to DEBUG
curl -X POST http://localhost:8080/actuator/loggers/com.midlevel.orderfulfillment \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## 🔍 Debugging Production Issues

### Scenario: "Order creation is slow"

**Step 1: Check Metrics**
```bash
curl http://localhost:8080/actuator/metrics/orders.creation.duration
```
Look at percentiles: p50, p95, p99, max

**Step 2: Check Logs with Correlation ID**
```bash
# Get correlation ID from response header
curl -v http://localhost:8080/api/orders -d '...'
# X-Correlation-Id: a1b2c3d4-...

# Search logs for that correlation ID
grep "a1b2c3d4" logs/application.log
```

**Step 3: Check Database Health**
```bash
curl http://localhost:8080/actuator/health/db
```

**Step 4: Check Thread Dump**
```bash
curl http://localhost:8080/actuator/threaddump > threads.txt
```
Look for blocked threads or deadlocks

### Scenario: "Events not being consumed from Kafka"

**Step 1: Check Kafka Health**
```bash
curl http://localhost:8080/actuator/health/kafka
```

**Step 2: Check Event Metrics**
```bash
curl http://localhost:8080/actuator/metrics/events.published.total
curl http://localhost:8080/actuator/metrics/events.consumed.total
```
Compare published vs consumed - gap indicates lag

**Step 3: Check Failure Count**
```bash
curl http://localhost:8080/actuator/metrics/events.failures.total
```

**Step 4: Search Logs**
```bash
grep "Failed to" logs/application.log | grep "event"
```

---

## 🚀 Next Steps

### Remaining Day 10 Tasks

1. **Add Logging to More Components**
   - KafkaEventPublisher
   - KafkaEventConsumer
   - NotificationService
   - Controllers

2. **Add More Metrics**
   - Kafka consumer lag gauge
   - Order state distribution gauge
   - Event processing duration timer

3. **Integration Testing**
   - Test actuator endpoints
   - Verify metrics increment
   - Validate correlation ID propagation

4. **Documentation**
   - Monitoring runbook
   - Alert thresholds
   - Dashboard configurations

---

## 📈 Grafana Dashboard (Future Enhancement)

Once Prometheus is scraping `/actuator/prometheus`, create Grafana dashboard with:

**Panels:**
- Order creation rate (orders/sec)
- Order creation latency (p50, p95, p99)
- HTTP request rate and latency
- JVM memory usage
- Kafka event throughput
- Error rates
- Health status timeline

**Alerts:**
- Order creation latency > 500ms
- Event failures > 10/min
- Kafka health DOWN
- Database health DOWN
- JVM memory > 80%

---

## ✅ Day 10 Progress: 60% Complete

**Completed:**
- ✅ Dependencies and configuration
- ✅ Structured JSON logging
- ✅ Correlation ID filter
- ✅ Custom business metrics
- ✅ Health indicators
- ✅ Enhanced OrderService logging

**Remaining:**
- ⏳ Add logging to remaining components
- ⏳ Add metrics to event processing
- ⏳ Integration tests
- ⏳ Documentation and runbooks

**Estimated Time to Complete:** 1-2 hours
