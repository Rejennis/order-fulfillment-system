package com.midlevel.orderfulfillment.domain.port;

import com.midlevel.orderfulfillment.domain.model.Order;

/**
 * Port for sending notifications about order events.
 * 
 * <p>This is a domain port (driven/output port) that defines the contract for 
 * notification delivery without specifying implementation details. Adapters can
 * implement this interface using various technologies (email, SMS, push notifications,
 * webhooks, etc.).</p>
 * 
 * <p><strong>Hexagonal Architecture Note:</strong> This interface lives in the domain
 * layer and is implemented by adapters in the infrastructure layer. The domain
 * doesn't depend on notification implementation details.</p>
 * 
 * @since Day 8 - Notification System
 */
public interface NotificationPort {
    
    /**
     * Send a notification when an order is created.
     * 
     * @param order the newly created order
     */
    void sendOrderCreatedNotification(Order order);
    
    /**
     * Send a notification when an order is paid.
     * 
     * @param order the order that was paid
     */
    void sendOrderPaidNotification(Order order);
    
    /**
     * Send a notification when an order is shipped.
     * 
     * @param order the order that was shipped
     */
    void sendOrderShippedNotification(Order order);
    
    /**
     * Send a notification when an order is cancelled.
     * 
     * @param order the order that was cancelled
     */
    void sendOrderCancelledNotification(Order order);
}
