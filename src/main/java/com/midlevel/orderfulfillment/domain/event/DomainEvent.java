package com.midlevel.orderfulfillment.domain.event;

import java.time.Instant;

/**
 * Base class for all domain events.
 * 
 * Domain Events represent something that happened in the domain that domain experts care about.
 * They are named in past tense (OrderCreated, not CreateOrder) because they represent facts.
 * 
 * Benefits of Domain Events:
 * 1. Decoupling: Different parts of the system can react to events independently
 * 2. Audit Trail: Events provide a history of what happened
 * 3. Integration: Events can be published to external systems
 * 4. Eventual Consistency: Allow async processing without blocking the main flow
 * 
 * DDD Pattern: Domain events are part of the Ubiquitous Language
 */
public abstract class DomainEvent {
    
    private final String eventId;
    private final Instant occurredAt;
    
    protected DomainEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    /**
     * Returns the aggregate ID that this event relates to.
     * Useful for event sourcing and correlating events.
     */
    public abstract String getAggregateId();
}
