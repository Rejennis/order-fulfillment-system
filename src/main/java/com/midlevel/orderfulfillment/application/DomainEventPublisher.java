package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Service responsible for publishing domain events to Spring's event system.
 * 
 * IMPLEMENTATION TIMELINE:
 * - Implemented in Day 4 (Actually Day 7: Domain Events in mentor program)
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
    
    /**
     * Publish all domain events from an Order aggregate.
     * This is a convenience method that extracts and publishes events from the aggregate.
     * 
     * @param order the order aggregate with pending domain events
     */
    public void publishEvents(com.midlevel.orderfulfillment.domain.model.Order order) {
        publishAll(order.getDomainEvents());
        order.clearDomainEvents();
    }
}
