package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Service responsible for publishing domain events to Spring's event system.
 * 
 * This bridges the gap between:
 * - Domain layer (which shouldn't depend on Spring)
 * - Infrastructure layer (Spring ApplicationEventPublisher)
 * 
 * Why this pattern?
 * 1. Domain remains framework-agnostic
 * 2. Events can be published to different systems (Spring Events, Kafka, RabbitMQ)
 * 3. Testability - can mock event publishing in tests
 */
@Component
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    /**
     * Publish a single domain event.
     */
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
    
    /**
     * Publish multiple domain events.
     */
    public void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
