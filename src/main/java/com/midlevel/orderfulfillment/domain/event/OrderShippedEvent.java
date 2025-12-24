package com.midlevel.orderfulfillment.domain.event;

import java.time.Instant;

/**
 * Event published when an order is shipped.
 * 
 * This event triggers:
 * - Shipping notification to customer with tracking info
 * - Inventory deduction (if not done earlier)
 * - Expected delivery date calculation
 * - Customer service team notification
 */
public class OrderShippedEvent extends DomainEvent {
    
    private final String orderId;
    private final String customerId;
    private final Instant shippedAt;
    
    public OrderShippedEvent(String orderId, String customerId, Instant shippedAt) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.shippedAt = shippedAt;
    }
    
    @Override
    public String getAggregateId() {
        return orderId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Instant getShippedAt() {
        return shippedAt;
    }
    
    @Override
    public String toString() {
        return "OrderShippedEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", shippedAt=" + shippedAt +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
