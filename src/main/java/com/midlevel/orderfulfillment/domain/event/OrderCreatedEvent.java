package com.midlevel.orderfulfillment.domain.event;

import com.midlevel.orderfulfillment.domain.model.Money;

import java.util.List;

/**
 * Event published when a new order is created.
 * 
 * This event signals that a customer has placed an order.
 * Other parts of the system can react to this:
 * - Send confirmation email
 * - Reserve inventory
 * - Notify warehouse
 * - Update analytics
 */
public class OrderCreatedEvent extends DomainEvent {
    
    private final String orderId;
    private final String customerId;
    private final Money totalAmount;
    private final int itemCount;
    
    public OrderCreatedEvent(String orderId, String customerId, Money totalAmount, int itemCount) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
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
    
    public int getItemCount() {
        return itemCount;
    }
    
    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", totalAmount=" + totalAmount +
                ", itemCount=" + itemCount +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
