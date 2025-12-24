package com.midlevel.orderfulfillment.domain.event;

import com.midlevel.orderfulfillment.domain.model.Money;

import java.time.Instant;

/**
 * Event published when an order is paid.
 * 
 * This is a critical business event that triggers:
 * - Payment confirmation to customer
 * - Authorization to ship
 * - Financial reconciliation
 * - Commission calculation for sales
 */
public class OrderPaidEvent extends DomainEvent {
    
    private final String orderId;
    private final String customerId;
    private final Money totalAmount;
    private final Instant paidAt;
    
    public OrderPaidEvent(String orderId, String customerId, Money totalAmount, Instant paidAt) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.paidAt = paidAt;
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
    
    public Money getTotalAmount() {
        return totalAmount;
    }
    
    public Instant getPaidAt() {
        return paidAt;
    }
    
    @Override
    public String toString() {
        return "OrderPaidEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", totalAmount=" + totalAmount +
                ", paidAt=" + paidAt +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
