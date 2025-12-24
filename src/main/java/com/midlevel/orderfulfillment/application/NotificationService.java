package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Application service for coordinating notification delivery.
 * 
 * <p>This service acts as a coordinator between the domain events and the notification
 * infrastructure. It uses the NotificationPort to send notifications, making it
 * implementation-agnostic (email, SMS, push, etc.).</p>
 * 
 * <p><strong>Async Processing:</strong> All notification methods are annotated with
 * {@code @Async} to ensure notification delivery doesn't block the main business flow.
 * If notification fails, the order processing still succeeds.</p>
 * 
 * <p><strong>Error Handling:</strong> Notification failures are logged but don't cause
 * transaction rollback. This is intentional - we don't want a failed email to prevent
 * order processing.</p>
 * 
 * @since Day 8 - Notification System
 */
@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationPort notificationPort;
    
    public NotificationService(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }
    
    /**
     * Notify customer that their order was created.
     * 
     * <p>Runs asynchronously to avoid blocking order creation.</p>
     * 
     * @param order the newly created order
     */
    @Async
    public void notifyOrderCreated(Order order) {
        try {
            log.info("Sending order created notification for order: {}", order.getOrderId());
            notificationPort.sendOrderCreatedNotification(order);
        } catch (Exception e) {
            // Log error but don't propagate - notification failure shouldn't fail order creation
            log.error("Failed to send order created notification for order: {}", 
                    order.getOrderId(), e);
        }
    }
    
    /**
     * Notify customer that their payment was confirmed.
     * 
     * <p>Runs asynchronously to avoid blocking payment processing.</p>
     * 
     * @param order the paid order
     */
    @Async
    public void notifyOrderPaid(Order order) {
        try {
            log.info("Sending order paid notification for order: {}", order.getOrderId());
            notificationPort.sendOrderPaidNotification(order);
        } catch (Exception e) {
            log.error("Failed to send order paid notification for order: {}", 
                    order.getOrderId(), e);
        }
    }
    
    /**
     * Notify customer that their order was shipped.
     * 
     * <p>Runs asynchronously to avoid blocking shipping operations.</p>
     * 
     * @param order the shipped order
     */
    @Async
    public void notifyOrderShipped(Order order) {
        try {
            log.info("Sending order shipped notification for order: {}", order.getOrderId());
            notificationPort.sendOrderShippedNotification(order);
        } catch (Exception e) {
            log.error("Failed to send order shipped notification for order: {}", 
                    order.getOrderId(), e);
        }
    }
    
    /**
     * Notify customer that their order was cancelled.
     * 
     * <p>Runs asynchronously to avoid blocking cancellation.</p>
     * 
     * @param order the cancelled order
     */
    @Async
    public void notifyOrderCancelled(Order order) {
        try {
            log.info("Sending order cancelled notification for order: {}", order.getOrderId());
            notificationPort.sendOrderCancelledNotification(order);
        } catch (Exception e) {
            log.error("Failed to send order cancelled notification for order: {}", 
                    order.getOrderId(), e);
        }
    }
}
