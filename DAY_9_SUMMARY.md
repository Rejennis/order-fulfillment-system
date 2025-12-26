# Day 9: Message Queue Integration with Kafka
**Date:** December 26, 2025  
**Focus:** Production Event Streaming - Transitioning from Spring Events to Kafka  
**Mentor Program Phase:** Week 3 - Event-Driven Architecture at Scale

---

## 📋 Executive Summary

Today we evolve from Spring's in-memory event bus to **Apache Kafka**, a distributed event streaming platform used in production systems worldwide. This transition demonstrates:

✅ **Event Streaming** - Replace in-process events with persistent message queues  
✅ **Async at Scale** - Decouple services for independent scaling  
✅ **Durability** - Events survive application restarts  
✅ **Replay Capability** - Re-process events for new consumers  
✅ **Production Patterns** - Idempotency, retry logic, error handling

**Key Learning:** Understanding when to use Spring Events vs Kafka in real systems.

---

## 🎯 Learning Objectives

### 1. Kafka Fundamentals
- ✅ Understand Kafka architecture (brokers, topics, partitions)
- ✅ Set up Kafka with Docker Compose
- ✅ Configure producers and consumers in Spring Boot
- ✅ Handle serialization/deserialization

### 2. Event-Driven Patterns
- ✅ Publish domain events to Kafka topics
- ✅ Consume events from multiple consumers
- ✅ Implement idempotent consumers
- ✅ Handle consumer failures and retries

### 3. Production Readiness
- ✅ Configure proper error handling
- ✅ Implement dead letter queues
- ✅ Add observability (metrics, logging)
- ✅ Test Kafka integration

---

## 🤔 Why Kafka? (vs Spring Events)

### Spring Events (Current Implementation)
**Pros:**
- Simple to implement
- No external dependencies
- Fast (in-memory)
- Perfect for monoliths

**Cons:**
- ❌ Events lost on restart
- ❌ Cannot scale horizontally (all listeners in one app)
- ❌ No replay capability
- ❌ No durability guarantees

### Kafka (What We're Building)
**Pros:**
- ✅ Durable (events persist to disk)
- ✅ Scalable (multiple consumers, partitions)
- ✅ Replay events for new consumers
- ✅ Decouples services (microservices ready)
- ✅ Industry standard for event streaming

**Cons:**
- More complex to set up
- Requires infrastructure (Kafka cluster)
- Network latency vs in-memory
- Operational overhead

### When to Use What?

| Scenario | Use Spring Events | Use Kafka |
|----------|-------------------|-----------|
| Monolithic app, simple notifications | ✅ | ❌ |
| Need durability & replay | ❌ | ✅ |
| Microservices architecture | ❌ | ✅ |
| High throughput (millions of events) | ❌ | ✅ |
| Need event sourcing | ❌ | ✅ |
| Prototyping/learning | ✅ | ❌ |

**Today's Goal:** Add Kafka as an **option**, keeping Spring Events for comparison. Real-world systems often have both!

---

## 🏗️ Architecture Evolution

### Before (Day 8) - Spring Events
```
OrderService
    └─→ publishes OrderCreatedEvent (in-memory)
            └─→ OrderEventListener
                    └─→ NotificationService
```

**Flow:**
1. Transaction commits
2. Event published to ApplicationEventPublisher
3. Listener receives event (same JVM, different thread)
4. Notification sent

**Problem:** If app crashes after commit but before notification, event is LOST.

---

### After (Day 9) - Kafka
```
OrderService
    └─→ publishes OrderCreatedEvent to Kafka topic
            ↓
    [Kafka Broker - Persistent Storage]
            ↓
    Multiple Consumers (can be different services):
        ├─→ NotificationConsumer (sends emails)
        ├─→ AnalyticsConsumer (tracks metrics)
        ├─→ WarehouseConsumer (manages inventory)
        └─→ AuditConsumer (logs for compliance)
```

**Flow:**
1. Transaction commits
2. Event published to Kafka topic
3. Kafka stores event durably
4. Consumer fetches event (pull model)
5. Consumer processes and commits offset
6. Event stays in Kafka (can be replayed)

**Benefits:**
- Events persist even if consumers are down
- New consumers can read historical events
- Services scale independently

---

## 📦 What We're Building Today

### 1. Infrastructure Setup
- [ ] Add Kafka & Zookeeper to docker-compose.yml
- [ ] Add Kafka dependencies to pom.xml
- [ ] Configure Kafka properties in application.yml

### 2. Producer Implementation
- [ ] Create KafkaEventPublisher (replaces DomainEventPublisher)
- [ ] Configure Kafka producer with proper serialization
- [ ] Publish domain events to topics
- [ ] Add error handling and retries

### 3. Consumer Implementation
- [ ] Create KafkaEventConsumer for notifications
- [ ] Implement idempotent processing
- [ ] Add retry logic with exponential backoff
- [ ] Configure dead letter topic for failures

### 4. Observability
- [ ] Add correlation IDs for tracing
- [ ] Log event publishing and consumption
- [ ] Add metrics (events published, consumed, failed)

### 5. Testing
- [ ] Integration tests with Testcontainers (Kafka)
- [ ] Test producer-consumer flow
- [ ] Test failure scenarios
- [ ] Test idempotency

---

## 🔧 Implementation Plan

### Step 1: Docker Compose - Add Kafka
```yaml
# Add to docker-compose.yml
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### Step 2: Maven Dependencies
```xml
<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<!-- Testing -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Step 3: Application Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Strongest durability
      retries: 3
    consumer:
      group-id: order-fulfillment-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false  # Manual commit for safety
```

### Step 4: Create Kafka Topics
Create dedicated topics for each event type:
- `order.created`
- `order.paid`
- `order.shipped`
- `order.cancelled`
- `order.events.dlq` (Dead Letter Queue)

### Step 5: Kafka Producer
```java
@Component
public class KafkaEventPublisher {
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    public void publish(DomainEvent event) {
        String topic = determineTopicFromEvent(event);
        String key = extractAggregateId(event);
        
        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event", ex);
                    // Handle failure (retry, DLQ, alert)
                } else {
                    log.info("Event published: topic={}, key={}", topic, key);
                }
            });
    }
}
```

### Step 6: Kafka Consumer
```java
@Component
public class KafkaEventConsumer {
    
    @KafkaListener(topics = "order.created", groupId = "notification-service")
    public void handleOrderCreated(OrderCreatedEvent event,
                                   Acknowledgment acknowledgment) {
        try {
            // Idempotency check
            if (alreadyProcessed(event.getEventId())) {
                log.info("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Process event
            notificationService.notifyOrderCreated(event);
            
            // Mark as processed
            markAsProcessed(event.getEventId());
            
            // Manually commit offset
            acknowledgment.acknowledge();
            
        } catch (Exception ex) {
            log.error("Failed to process event", ex);
            // Don't acknowledge - will retry
            // Eventually goes to DLQ
        }
    }
}
```

---

## 🎯 Key Concepts

### 1. Topics & Partitions
- **Topic:** Category of events (e.g., "order.created")
- **Partition:** Subdivision for parallelism
- **Key:** Determines partition (same orderId → same partition → ordering guaranteed)

### 2. Producer Acknowledgments (acks)
- `acks=0` - Fire and forget (fast, may lose data)
- `acks=1` - Leader acknowledges (balance)
- `acks=all` - All replicas acknowledge (slow, most durable) ← We use this

### 3. Consumer Groups
- Multiple consumers with same group ID share partitions
- Each partition consumed by only one consumer in group
- Enables horizontal scaling

### 4. Offsets
- Position in partition (like a bookmark)
- Consumer tracks offset
- Can rewind and replay events

### 5. Idempotency
**Problem:** Consumer crashes after processing but before committing offset → event reprocessed

**Solution:** Store processed event IDs in database
```java
if (alreadyProcessed(eventId)) {
    acknowledge();  // Skip reprocessing
    return;
}
// Process...
markAsProcessed(eventId);
acknowledge();
```

### 6. Dead Letter Queue (DLQ)
Events that fail after N retries go to DLQ for manual inspection.

---

## 📊 Comparison: Spring Events vs Kafka

| Aspect | Spring Events | Kafka |
|--------|---------------|-------|
| **Latency** | Microseconds | Milliseconds |
| **Durability** | None (in-memory) | Persistent (disk) |
| **Scalability** | Single JVM | Distributed |
| **Replay** | No | Yes |
| **Ordering** | Within thread | Per partition |
| **Failure Handling** | Lost on crash | Retries + DLQ |
| **Setup Complexity** | Trivial | Moderate |
| **Use Case** | Monolith | Microservices |

---

## 🧪 Testing Strategy

### Unit Tests
- Test event serialization/deserialization
- Test topic name mapping
- Test idempotency logic

### Integration Tests (Testcontainers)
```java
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @Test
    void shouldPublishAndConsumeEvent() {
        // Publish event
        publisher.publish(new OrderCreatedEvent(...));
        
        // Wait for consumption
        await().atMost(5, SECONDS)
               .until(() -> notificationSent);
        
        // Verify
        verify(notificationService).notifyOrderCreated(any());
    }
}
```

### Manual Testing
```bash
# Start Kafka
docker-compose up -d

# Check topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume events (manual observation)
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created \
  --from-beginning
```

---

## 📝 Configuration Patterns

### Development vs Production

**Development (docker-compose):**
```yaml
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1  # Single broker OK
```

**Production (managed Kafka):**
```yaml
spring:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092  # Multiple brokers
    producer:
      acks: all
      retries: 5
      compression-type: snappy  # Reduce network bandwidth
    consumer:
      max-poll-records: 100  # Batch processing
      session-timeout-ms: 30000
```

---

## 🎓 Interview Talking Points

> "I implemented event-driven architecture using both Spring Events and Kafka. Spring Events work great for monolithic applications where speed is critical and you don't need durability. Kafka is essential for microservices where events need to survive restarts, enable replay, and scale horizontally."

> "For Kafka consumers, I implemented idempotency by storing processed event IDs in the database. This prevents duplicate processing when consumers retry after failures. I also configured dead letter queues for events that fail after multiple retries."

> "I used manual offset commits instead of auto-commit to ensure exactly-once semantics. The consumer only commits the offset after successfully processing and persisting the event state."

---

## 🚀 Next Steps (Day 10)

Tomorrow we'll focus on **Observability & Monitoring**:
- [ ] Structured logging with correlation IDs
- [ ] Metrics with Micrometer (order counts, Kafka lag)
- [ ] Distributed tracing
- [ ] Health checks
- [ ] Dashboards

---

## 📚 Resources

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Kafka Tutorials](https://kafka-tutorials.confluent.io/)
- [Testcontainers Kafka Module](https://www.testcontainers.org/modules/kafka/)

---

**Day 9 Status:** 🚧 In Progress  
**Deliverable:** Kafka-based event streaming system with Spring Events as fallback  
**Time Investment:** 3-4 hours  
**Complexity:** High - Production messaging infrastructure
