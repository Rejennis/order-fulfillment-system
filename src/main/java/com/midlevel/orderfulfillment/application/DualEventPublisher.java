package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.adapter.out.kafka.KafkaEventPublisher;
import com.midlevel.orderfulfillment.domain.event.DomainEvent;
import com.midlevel.orderfulfillment.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dual Event Publisher - Supports both Spring Events and Kafka.
 * 
 * This adapter allows switching between event publishing strategies:
 * - Spring Events (in-memory): Fast, simple, monolith-friendly
 * - Kafka (persistent): Durable, scalable, microservices-ready
 * 
 * Configuration via application.yml:
 * ```
 * events:
 *   publisher: kafka  # or "spring" for Spring Events
 * ```
 * 
 * Why support both?
 * - Development: Spring Events are easier for local dev/testing
 * - Production: Kafka provides durability and scalability
 * - Migration: Gradually move from monolith to microservices
 * - Learning: Understand trade-offs between approaches
 * 
 * This demonstrates the Adapter Pattern:
 * - Domain defines what it needs (publish events)
 * - Infrastructure provides different implementations
 * - Application code doesn't care which is used
 */
@Component
public class DualEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(DualEventPublisher.class);
    
    private final DomainEventPublisher springEventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final String publisherType;
    
    public DualEventPublisher(
            DomainEventPublisher springEventPublisher,
            KafkaEventPublisher kafkaEventPublisher,
            @Value("${events.publisher:spring}") String publisherType) {
        this.springEventPublisher = springEventPublisher;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.publisherType = publisherType.toLowerCase();
        
        log.info("🚀 DualEventPublisher initialized with publisher type: {}", publisherType);
    }
    
    /**
     * Publish all domain events from an aggregate.
     * 
     * Routes to Spring Events or Kafka based on configuration.
     */
    public void publishEvents(Order order) {
        if (order.getDomainEvents().isEmpty()) {
            return;
        }
        
        log.info("📢 Publishing {} events using {} publisher", 
                 order.getDomainEvents().size(), publisherType);
        
        if ("kafka".equals(publisherType)) {
            publishToKafka(order);
        } else {
            publishToSpring(order);
        }
    }
    
    /**
     * Publish a single domain event.
     */
    public void publish(DomainEvent event) {
        if ("kafka".equals(publisherType)) {
            kafkaEventPublisher.publish(event);
        } else {
            springEventPublisher.publish(event);
        }
    }
    
    /**
     * Publish events to Spring's ApplicationEventPublisher (in-memory).
     */
    private void publishToSpring(Order order) {
        springEventPublisher.publishEvents(order);
    }
    
    /**
     * Publish events to Kafka (persistent message queue).
     */
    private void publishToKafka(Order order) {
        order.getDomainEvents().forEach(event -> {
            try {
                kafkaEventPublisher.publish(event);
            } catch (Exception ex) {
                log.error("Failed to publish event to Kafka: {}", 
                         event.getClass().getSimpleName(), ex);
                // Fallback to Spring Events if Kafka fails?
                // Or let it fail and rely on Kafka's retry mechanism?
            }
        });
    }
}
