# Data Flow Patterns in Software Architecture

This document explores various data flow patterns and compares them with the patterns currently implemented in this Order Fulfillment System.

## Overview

There are several data flow patterns in software architecture, each suited for different use cases. This system currently implements **Synchronous Request-Response** and **Asynchronous Event-Driven** patterns.

---

## 1. Synchronous Request-Response ✅ (Implemented)

### Pattern Description
```
Client → HTTP Request → Server → Process → HTTP Response → Client
```

### Current Implementation
- **Location:** REST API endpoints in `OrderController`
- **Technology:** Spring Web MVC
- **Characteristics:** 
  - Client waits for response
  - Blocking operation
  - Immediate feedback

### Example
```java
@PostMapping("/api/orders")
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    Order order = mapper.toDomain(request);
    Order savedOrder = orderService.createOrder(order);
    OrderResponse response = mapper.toResponse(savedOrder);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### Use Cases
- CRUD operations
- User-initiated actions requiring immediate feedback
- Simple query operations

### Pros
- ✅ Simple to understand and implement
- ✅ Predictable behavior
- ✅ Easy error handling
- ✅ Good for transactions

### Cons
- ❌ Blocks client until completion
- ❌ Not suitable for long-running operations
- ❌ Limited scalability for high concurrency

---

## 2. Asynchronous Event-Driven ✅ (Implemented)

### Pattern Description
```
Action → Event Published → Event Bus → Multiple Listeners (async)
```

### Current Implementation
- **Location:** Domain events with `@EventListener` and `@Async`
- **Technology:** Spring Application Events
- **Characteristics:**
  - Fire-and-forget
  - Non-blocking
  - Multiple listeners can react to same event

### Example
```java
// Publishing
@Transactional
public Order createOrder(Order order) {
    Order savedOrder = orderRepository.save(order);
    eventPublisher.publishEvents(savedOrder);  // Async events
    return savedOrder;
}

// Consuming
@EventListener
@Async
public void handleOrderCreated(OrderCreatedEvent event) {
    notificationService.notifyOrderCreated(order);
    reserveInventory(event);
}
```

### Use Cases
- Notifications (email, SMS)
- Audit logging
- Analytics tracking
- Side effects that don't affect main transaction

### Pros
- ✅ Decouples components
- ✅ Multiple listeners without modifying publisher
- ✅ Improves response time
- ✅ Easy to add new reactions

### Cons
- ❌ Events are in-memory (lost on crash)
- ❌ No guaranteed delivery
- ❌ Limited to single JVM
- ❌ Debugging can be harder

---

## 3. Message Queue / Broker Pattern ❌ (Not Implemented)

### Pattern Description
```
Producer → Message Queue (RabbitMQ/Kafka) → Consumer(s)
```

### How It Would Look
```
Order Service → [Kafka Topic: order-created] → Inventory Service
                                             → Shipping Service
                                             → Analytics Service
                                             → Notification Service
```

### Technologies
- **Message Brokers:** RabbitMQ, Apache Kafka, Amazon SQS, Azure Service Bus
- **Protocols:** AMQP, MQTT

### Example Implementation
```java
// Producer
@Service
public class OrderKafkaProducer {
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created", event.getOrderId(), event);
    }
}

// Consumer (separate service)
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
    inventoryService.reserveItems(event.getOrderId());
}
```

### Use Cases
- Microservices communication
- Distributed systems
- Guaranteed delivery required
- High-volume event processing
- System decoupling across applications

### Pros
- ✅ Durable messages (survive crashes)
- ✅ Guaranteed delivery
- ✅ Works across multiple applications/services
- ✅ Built-in retry mechanisms
- ✅ Load balancing across consumers
- ✅ Message persistence

### Cons
- ❌ Additional infrastructure (broker)
- ❌ Increased complexity
- ❌ Network latency
- ❌ Eventual consistency challenges
- ❌ Operational overhead

### When to Add
- Splitting into microservices
- Need guaranteed event delivery
- Multiple independent applications need same events
- High availability requirements

---

## 4. CQRS (Command Query Responsibility Segregation) 🟡 (Partially Implemented)

### Pattern Description
```
Write Model (Commands)  →  Event Store  →  Read Model (Queries)
```

### Current State
You have **CQRS-lite**:
- Commands: `createOrder()`, `markOrderAsPaid()`, `markOrderAsShipped()`
- Queries: `findById()`, `findByCustomerId()`, `findByStatus()`
- Same database for both

### Full CQRS Would Be
```
Command Side:                    Event Store:              Query Side:
POST /orders                     OrderCreatedEvent         Optimized Read DB
  ↓                                  ↓                         ↓
Write to Event Store  ────────→  Event Handlers  ────→  Update Materialized View
(PostgreSQL Events)                                      (MongoDB/Redis/Elasticsearch)
```

### Example Implementation
```java
// Command Side - Stores only events
public class OrderCommandService {
    public void createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(cmd.getCustomerId(), cmd.getItems());
        eventStore.append("order-" + order.getId(), order.getDomainEvents());
    }
}

// Query Side - Optimized for reads
@EventListener
public void on(OrderCreatedEvent event) {
    OrderReadModel readModel = new OrderReadModel(event);
    mongoRepository.save(readModel);  // Separate database
}

// Query from read model
public OrderView getOrder(String orderId) {
    return mongoRepository.findById(orderId);  // Fast reads
}
```

### Use Cases
- Read and write patterns differ significantly
- High read-to-write ratio
- Complex queries needed
- Need to scale reads independently from writes
- Event sourcing architecture

### Pros
- ✅ Optimize reads and writes separately
- ✅ Scale independently
- ✅ Multiple read models for different use cases
- ✅ Complete audit trail (if using event sourcing)

### Cons
- ❌ Significant complexity increase
- ❌ Eventual consistency
- ❌ Data duplication
- ❌ More infrastructure to manage

---

## 5. Streaming Data Flow ❌ (Not Implemented)

### Pattern Description
```
Continuous Stream → Process → Transform → Aggregate → Output
```

### How It Would Look
```
Order Events Stream (Kafka)
  → Filter (orders > $100)
  → Window (5-minute windows)
  → Aggregate (count, sum, average)
  → Alert if > 1000 orders/5min
  → Real-time dashboard update
```

### Technologies
- **Stream Processing:** Kafka Streams, Apache Flink, Spark Streaming
- **Complex Event Processing:** Esper, Apache Storm

### Example Implementation
```java
// Kafka Streams
StreamsBuilder builder = new StreamsBuilder();
KStream<String, Order> orders = builder.stream("orders");

orders
    .filter((key, order) -> order.getTotalAmount() > 100)
    .groupByKey()
    .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
    .count()
    .toStream()
    .foreach((window, count) -> {
        if (count > 1000) {
            alertService.sendAlert("High order volume: " + count);
        }
    });
```

### Use Cases
- Real-time analytics
- Fraud detection
- Monitoring and alerting
- Live dashboards
- Real-time recommendations
- IoT data processing

### Pros
- ✅ Process data as it arrives
- ✅ Low latency insights
- ✅ Handles high throughput
- ✅ Temporal operations (windowing)

### Cons
- ❌ Complex to implement
- ❌ Requires streaming infrastructure
- ❌ State management challenges
- ❌ Debugging is difficult

---

## 6. Batch Processing ❌ (Not Implemented)

### Pattern Description
```
Scheduled Job → Read Batch → Process → Write Batch → Next Batch
```

### How It Would Look
```
Daily Job (2 AM)
  → Read all SHIPPED orders from yesterday
  → Generate invoice PDFs
  → Send to accounting system
  → Mark invoices as generated
  → Send summary report
```

### Technologies
- **Frameworks:** Spring Batch, Quartz Scheduler
- **Orchestration:** Apache Airflow, Jenkins

### Example Implementation
```java
@Configuration
@EnableBatchProcessing
public class InvoiceBatchConfig {
    
    @Bean
    public Job invoiceJob(JobBuilderFactory jobs, Step generateInvoices) {
        return jobs.get("invoiceJob")
            .start(generateInvoices)
            .build();
    }
    
    @Bean
    public Step generateInvoices(StepBuilderFactory steps) {
        return steps.get("generateInvoices")
            .<Order, Invoice>chunk(100)
            .reader(orderReader())
            .processor(invoiceProcessor())
            .writer(invoiceWriter())
            .build();
    }
}

// Scheduled trigger
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void runInvoiceGeneration() {
    jobLauncher.run(invoiceJob, new JobParameters());
}
```

### Use Cases
- End-of-day reporting
- Data migration
- Bulk updates
- Invoice generation
- Data warehouse ETL
- Periodic cleanup tasks

### Pros
- ✅ Efficient for large datasets
- ✅ Better resource utilization (off-peak hours)
- ✅ Transactional chunks
- ✅ Restart capability
- ✅ Progress tracking

### Cons
- ❌ Not real-time
- ❌ Delayed feedback
- ❌ Scheduling complexity
- ❌ Long-running jobs can fail

---

## 7. Saga Pattern (Distributed Transactions) ❌ (Not Implemented)

### Pattern Description
Long-running transactions across multiple services with compensation logic.

### Orchestration Saga
```
Order Service (Orchestrator)
  ├→ 1. Create Order
  ├→ 2. PaymentService.charge()
  │     ├─ Success → 3. InventoryService.reserve()
  │     │              ├─ Success → 4. ShippingService.ship()
  │     │              └─ Failure → Compensate: PaymentService.refund()
  │     └─ Failure → OrderService.cancel()
```

### Choreography Saga (Event-Driven)
```
OrderCreated → PaymentService.charge()
            → PaymentCharged → InventoryService.reserve()
                            → InventoryReserved → ShippingService.ship()
                            → InventoryFailed → PaymentService.refund()
            → PaymentFailed → OrderService.cancel()
```

### Example Implementation
```java
// Orchestration Saga
@Service
public class OrderSaga {
    
    public void createOrder(CreateOrderCommand cmd) {
        String orderId = orderService.create(cmd);
        
        try {
            // Step 1: Charge payment
            PaymentResult payment = paymentService.charge(orderId, cmd.getAmount());
            
            try {
                // Step 2: Reserve inventory
                inventoryService.reserve(orderId, cmd.getItems());
                
                try {
                    // Step 3: Schedule shipping
                    shippingService.schedule(orderId);
                    orderService.complete(orderId);
                } catch (ShippingException e) {
                    // Compensate: unreserve inventory and refund
                    inventoryService.unreserve(orderId);
                    paymentService.refund(payment.getTransactionId());
                    orderService.cancel(orderId);
                }
            } catch (InventoryException e) {
                // Compensate: refund payment
                paymentService.refund(payment.getTransactionId());
                orderService.cancel(orderId);
            }
        } catch (PaymentException e) {
            // First step failed - just cancel order
            orderService.cancel(orderId);
        }
    }
}
```

### Use Cases
- Microservices architecture
- Distributed transactions
- Multi-step business processes
- Long-running workflows
- Cross-service operations

### Pros
- ✅ Distributed transaction handling
- ✅ Failure recovery with compensation
- ✅ Services remain independent
- ✅ No distributed locks

### Cons
- ❌ Complex to implement
- ❌ Compensation logic required
- ❌ Eventual consistency
- ❌ Difficult debugging

### Your System
Currently uses **local transactions** within a single service - much simpler!

---

## 8. Reactive Streams (Backpressure) ❌ (Not Implemented)

### Pattern Description
```
Publisher → Flow Control → Subscriber
         ← Backpressure ←
```

### How It Would Look
```java
// Reactive controller
@GetMapping(value = "/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<OrderResponse> streamOrders() {
    return orderRepository.findAllAsFlux()
        .filter(order -> order.getStatus() == OrderStatus.PAID)
        .map(mapper::toResponse)
        .delayElements(Duration.ofSeconds(1));
}

// Reactive repository
public interface ReactiveOrderRepository extends ReactiveCrudRepository<Order, String> {
    Flux<Order> findByStatus(OrderStatus status);
    Mono<Order> findById(String id);
}
```

### Technologies
- **Frameworks:** Spring WebFlux, Project Reactor
- **Libraries:** RxJava, Akka Streams

### Use Cases
- Streaming APIs (SSE, WebSocket)
- High-concurrency applications
- Resource-efficient services
- Real-time data feeds
- Chat applications

### Pros
- ✅ Non-blocking I/O
- ✅ Better resource utilization
- ✅ Handles backpressure
- ✅ Composable async operations

### Cons
- ❌ Steep learning curve
- ❌ Debugging is harder
- ❌ Not always needed
- ❌ Incompatible with blocking code

---

## 9. Pipeline Pattern (Chain of Processing) ❌ (Not Implemented)

### Pattern Description
```
Input → Stage 1 → Stage 2 → Stage 3 → Stage 4 → Output
```

### How It Would Look
```
Order Validation Pipeline:
Raw Order → ValidateItems → ValidateAddress → CheckInventory → ValidatePayment → Persist
```

### Example Implementation
```java
public interface ValidationStage {
    ValidationResult validate(Order order);
}

@Component
public class OrderValidationPipeline {
    private final List<ValidationStage> stages = List.of(
        new ItemValidationStage(),
        new AddressValidationStage(),
        new InventoryValidationStage(),
        new PaymentMethodValidationStage()
    );
    
    public ValidationResult validate(Order order) {
        for (ValidationStage stage : stages) {
            ValidationResult result = stage.validate(order);
            if (!result.isValid()) {
                return result;  // Stop on first failure
            }
        }
        return ValidationResult.success();
    }
}
```

### Use Cases
- Multi-step validation
- Data transformation pipelines
- ETL processes
- Request preprocessing
- Image/video processing

### Pros
- ✅ Clear separation of concerns
- ✅ Easy to add/remove stages
- ✅ Testable in isolation
- ✅ Reusable stages

### Cons
- ❌ Can add overhead
- ❌ Error handling complexity
- ❌ Debugging multiple stages

---

## 10. Pub-Sub with Topics ❌ (Not Implemented)

### Pattern Description
```
Publisher → Topic Broker → Subscribers
```

### How It Would Look
```
Order Service → [Topic: order.*]
                  ├→ order.created → Email Service
                  ├→ order.paid    → Inventory Service
                  ├→ order.shipped → Shipping Service
                  └→ order.*       → Analytics Service (all events)
```

### Technologies
- **Message Brokers:** Redis Pub/Sub, MQTT, Google Pub/Sub
- **Enterprise:** Apache Pulsar, Amazon SNS

### Example Implementation
```java
// Publisher
@Service
public class OrderEventPublisher {
    @Autowired
    private RedisTemplate<String, OrderEvent> redisTemplate;
    
    public void publish(String topic, OrderEvent event) {
        redisTemplate.convertAndSend(topic, event);
    }
}

// Subscriber
@Service
public class NotificationSubscriber {
    
    @RedisMessageListener(topics = "order.created")
    public void onOrderCreated(OrderCreatedEvent event) {
        emailService.sendConfirmation(event);
    }
    
    @RedisMessageListener(topics = "order.*")
    public void onAnyOrderEvent(OrderEvent event) {
        analyticsService.track(event);
    }
}
```

### Difference from Current Implementation
| Aspect | Current (Spring Events) | Pub-Sub Broker |
|--------|------------------------|----------------|
| Scope | Single JVM | Multiple applications |
| Durability | In-memory | Persistent |
| Delivery | Best effort | Guaranteed |
| Subscribers | Same app | Different apps |

### Use Cases
- Fan-out to multiple services
- Cross-application communication
- IoT device communication
- Real-time notifications
- Microservices events

---

## 11. ETL (Extract, Transform, Load) ❌ (Not Implemented)

### Pattern Description
```
Source DB → Extract → Transform → Load → Target DB/Warehouse
```

### How It Would Look
```
PostgreSQL (Orders)
  → Extract (nightly batch)
  → Transform (join with customers, calculate metrics)
  → Load → BigQuery/Snowflake (Analytics Warehouse)
```

### Example Implementation
```java
@Scheduled(cron = "0 0 3 * * *")  // 3 AM daily
public void etlOrdersToWarehouse() {
    // Extract
    List<Order> orders = orderRepository.findOrdersCreatedYesterday();
    
    // Transform
    List<OrderAnalyticsRecord> records = orders.stream()
        .map(this::enrichWithCustomerData)
        .map(this::calculateMetrics)
        .collect(Collectors.toList());
    
    // Load
    bigQueryService.bulkInsert("analytics.orders", records);
}

private OrderAnalyticsRecord enrichWithCustomerData(Order order) {
    Customer customer = customerService.findById(order.getCustomerId());
    return OrderAnalyticsRecord.builder()
        .orderId(order.getId())
        .customerId(customer.getId())
        .customerSegment(customer.getSegment())
        .orderAmount(order.getTotalAmount())
        .orderDate(order.getCreatedAt())
        .build();
}
```

### Use Cases
- Data warehousing
- Business intelligence
- Historical reporting
- Data consolidation
- Cross-system analytics

### Pros
- ✅ Centralized analytics
- ✅ Optimized for complex queries
- ✅ Historical trend analysis
- ✅ Separation from operational DB

### Cons
- ❌ Not real-time
- ❌ Data duplication
- ❌ Transformation complexity
- ❌ Maintenance overhead

---

## 12. GraphQL Data Flow ❌ (Not Implemented)

### Pattern Description
```
Client → Single GraphQL Query → Resolver → Multiple Data Sources
```

### How It Would Look
```
Query {
  order(id: "123") {
    orderId
    status
    customer {          ← Different service
      name
      email
    }
    items {             ← Different table
      product {         ← Different service
        name
        imageUrl
      }
      quantity
    }
  }
}
```

### Example Implementation
```java
@Component
public class OrderGraphQLResolver implements GraphQLQueryResolver {
    
    public Order order(String id) {
        return orderService.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
    
    public Customer customer(Order order) {
        return customerService.findById(order.getCustomerId());
    }
    
    public List<Product> products(OrderItem item) {
        return productService.findById(item.getProductId());
    }
}
```

### Difference from REST
| Aspect | REST | GraphQL |
|--------|------|---------|
| Endpoints | Multiple (/orders, /customers) | Single (/graphql) |
| Data fetching | Fixed structure | Client-specified |
| Over-fetching | Common | Eliminated |
| Round trips | Multiple | Single query |

### Use Cases
- Mobile apps (reduce bandwidth)
- Complex nested data
- Multiple clients with different needs
- Rapid frontend iteration
- Aggregating microservices

---

## 📊 Comparison Table

| Pattern | Sync/Async | Durability | Complexity | Your System | When to Use |
|---------|------------|------------|------------|-------------|-------------|
| **REST API** | Sync | No | Low | ✅ Has | CRUD, immediate response |
| **Domain Events** | Async | No | Low | ✅ Has | Side effects, notifications |
| **Message Queue** | Async | Yes | Medium | ❌ Missing | Microservices, reliability |
| **CQRS (Full)** | Both | Yes | High | 🟡 Partial | Read/write optimization |
| **Streaming** | Async | Yes | High | ❌ Missing | Real-time analytics |
| **Batch** | Sync | Yes | Medium | ❌ Missing | Scheduled reports |
| **Saga** | Both | Yes | High | ❌ Missing | Distributed transactions |
| **Reactive** | Async | No | High | ❌ Missing | High concurrency |
| **Pipeline** | Sync | No | Medium | ❌ Missing | Multi-step processing |
| **Pub-Sub** | Async | Yes | Medium | ❌ Missing | Fan-out events |
| **ETL** | Sync | Yes | Medium | ❌ Missing | Data warehousing |
| **GraphQL** | Sync | No | Medium | ❌ Missing | Flexible API queries |

---

## 🎯 Evolution Path for Your System

### Current State (Day 1-8)
```
✅ REST API (Synchronous)
✅ Domain Events (Asynchronous, in-memory)
✅ CQRS-lite (same database)
✅ Hexagonal Architecture
```

### Next Steps (If Scaling)

#### Step 1: Add Reliability
**Implement Message Queue (Kafka/RabbitMQ)**
- Replace in-memory events with durable messages
- Guarantee event delivery
- Enable retry mechanisms

```
Current:  Order → Spring Events → @EventListener
Enhanced: Order → Kafka → Multiple Services (durable)
```

#### Step 2: Separate Concerns
**Implement Batch Processing**
- Daily reports
- Invoice generation
- Data cleanup

```
Add: Spring Batch for scheduled tasks
```

#### Step 3: Scale Reads
**Implement Full CQRS**
- Separate read and write databases
- Optimize each for their purpose
- Use materialized views

```
Write: PostgreSQL (normalized)
Read:  MongoDB (denormalized) or Elasticsearch
```

#### Step 4: Go Distributed
**Implement Saga Pattern**
- When splitting into microservices
- For distributed transactions
- With compensation logic

```
Order Service ⟷ Payment Service ⟷ Inventory Service
(Each with rollback capability)
```

#### Step 5: Real-Time Analytics
**Implement Streaming**
- Real-time dashboards
- Fraud detection
- Live monitoring

```
Orders → Kafka → Flink/Spark Streaming → Dashboards
```

---

## 💡 Decision Guide

### Choose Synchronous (REST) When:
- ✅ User needs immediate feedback
- ✅ Simple CRUD operations
- ✅ Transaction must complete before response
- ✅ Client needs to know success/failure immediately

### Choose Asynchronous Events When:
- ✅ Side effects that can happen later
- ✅ Multiple independent actions
- ✅ Improve response time
- ✅ Loose coupling desired

### Choose Message Queue When:
- ✅ Going microservices
- ✅ Guaranteed delivery critical
- ✅ Multiple services need same events
- ✅ High availability required

### Choose CQRS When:
- ✅ Read patterns differ from write patterns
- ✅ High read-to-write ratio
- ✅ Complex queries needed
- ✅ Need to scale reads separately

### Choose Streaming When:
- ✅ Real-time analytics required
- ✅ High-volume events
- ✅ Temporal operations (windowing)
- ✅ Complex event processing

### Choose Batch When:
- ✅ Periodic processing sufficient
- ✅ Large dataset operations
- ✅ Off-peak processing acceptable
- ✅ Report generation

### Choose Saga When:
- ✅ Multiple services involved
- ✅ Distributed transaction needed
- ✅ Each step can fail independently
- ✅ Compensation logic required

---

## 🚀 Practical Recommendations

### For Your Current System
**Keep it simple!** Your current implementation is appropriate for:
- ✅ Single service/monolith
- ✅ Moderate traffic
- ✅ Team learning DDD/Hexagonal architecture

### When to Add Complexity

#### Add Message Queue When:
- Splitting into microservices
- Events getting lost is problematic
- Need cross-application communication

#### Add Batch Processing When:
- Need scheduled reports
- Have periodic maintenance tasks
- Generating invoices/statements

#### Add Full CQRS When:
- Read queries slow down writes
- Complex reporting queries impact performance
- Read/write traffic patterns very different

#### Add Streaming When:
- Need sub-second analytics
- Fraud detection required
- Real-time dashboards critical

---

## 📚 Further Reading

- **Event-Driven Architecture:** "Building Event-Driven Microservices" by Adam Bellemare
- **CQRS:** Greg Young's writings on CQRS and Event Sourcing
- **Sagas:** Chris Richardson's "Microservices Patterns"
- **Message Queues:** "Enterprise Integration Patterns" by Gregor Hohpe
- **Reactive:** "Reactive Design Patterns" by Roland Kuhn

---

## Summary

Your system has a **solid foundation** with synchronous REST API and asynchronous domain events. This is appropriate for most applications. Add complexity only when you have specific needs:

1. **Message Queue** → For reliability and microservices
2. **Batch** → For scheduled tasks
3. **Full CQRS** → For read/write optimization
4. **Streaming** → For real-time analytics
5. **Saga** → For distributed transactions

Remember: **Start simple, add complexity only when needed!** 🎯
