package com.midlevel.orderfulfillment.domain.event;

/**
 * Event published when an order is cancelled.
 * 
 * This event triggers:
 * - Cancellation notification to customer
 * - Refund processing (if paid)
 * - Inventory release
 * - Analytics update
 */
public class OrderCancelledEvent extends DomainEvent {
    
    private final String orderId;
    private final String customerId;
    private final String reason;
    
    public OrderCancelledEvent(String orderId, String customerId, String reason) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.reason = reason;
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
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", reason='" + reason + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
