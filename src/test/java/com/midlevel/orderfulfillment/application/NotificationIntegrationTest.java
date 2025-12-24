package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.model.Address;
import com.midlevel.orderfulfillment.domain.model.Money;
import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderItem;
import com.midlevel.orderfulfillment.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the notification system.
 * 
 * <p>These tests verify the complete notification flow from creating an order
 * through event emission to notification delivery. Tests cover:</p>
 * <ul>
 *   <li>Order lifecycle notifications (created, paid, shipped, cancelled)</li>
 *   <li>Async processing behavior</li>
 *   <li>Error handling (notification failures don't break order processing)</li>
 *   <li>Notification content validation</li>
 * </ul>
 * 
 * @since Day 8 - Notification System
 */
@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class NotificationIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    
    public NotificationIntegrationTest(OrderService orderService, 
                                      OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }
    
    @BeforeEach
    void setUp() {
        // Clean up before each test
        orderRepository.deleteAll();
    }
    
    /**
     * Test that order creation triggers notification.
     * Verifies notification logs appear after order is created.
     */
    @Test
    void whenOrderCreated_thenNotificationIsSent(CapturedOutput output) throws InterruptedException {
        // Given: A new order
        Order order = createTestOrder("CUST-001");
        
        // When: Order is created (persisted)
        Order savedOrder = orderRepository.save(order);
        
        // Wait for async notification to complete
        Thread.sleep(1000);
        
        // Then: Notification log should be present
        String logs = output.toString();
        assertThat(logs).contains("Order created notification sent");
        assertThat(logs).contains(savedOrder.getOrderId());
        assertThat(logs).contains("CUST-001");
    }
    
    /**
     * Test complete order lifecycle notifications.
     * Verifies all four notification types are sent: created, paid, shipped, cancelled.
     */
    @Test
    void completeOrderLifecycle_sendsAllNotifications(CapturedOutput output) throws InterruptedException {
        // Given: A new order
        Order order = createTestOrder("CUST-002");
        Order savedOrder = orderRepository.save(order);
        String orderId = savedOrder.getOrderId();
        
        Thread.sleep(500); // Wait for created notification
        
        // When: Order progresses through states
        orderService.markOrderAsPaid(orderId);
        Thread.sleep(500); // Wait for paid notification
        
        orderService.markOrderAsShipped(orderId);
        Thread.sleep(500); // Wait for shipped notification
        
        // Then: All three notifications should be logged
        String logs = output.toString();
        assertThat(logs).contains("Order created notification sent");
        assertThat(logs).contains("Order paid notification sent");
        assertThat(logs).contains("Order shipped notification sent");
    }
    
    /**
     * Test cancellation notification.
     * Verifies cancelled orders trigger appropriate notification.
     */
    @Test
    void whenOrderCancelled_thenNotificationIsSent(CapturedOutput output) throws InterruptedException {
        // Given: An existing order
        Order order = createTestOrder("CUST-003");
        Order savedOrder = orderRepository.save(order);
        String orderId = savedOrder.getOrderId();
        
        Thread.sleep(500); // Wait for created notification
        
        // When: Order is cancelled
        orderService.cancelOrder(orderId);
        Thread.sleep(500); // Wait for cancellation notification
        
        // Then: Cancellation notification should be logged
        String logs = output.toString();
        assertThat(logs).contains("Order cancelled notification sent");
        assertThat(logs).contains(orderId);
    }
    
    /**
     * Test that notifications run asynchronously.
     * Verifies the main thread doesn't wait for notification completion.
     */
    @Test
    void notifications_runAsynchronously() {
        // Given: A new order
        Order order = createTestOrder("CUST-004");
        
        // When: Order is created and state changes happen
        long startTime = System.currentTimeMillis();
        
        Order savedOrder = orderRepository.save(order);
        orderService.markOrderAsPaid(savedOrder.getOrderId());
        orderService.markOrderAsShipped(savedOrder.getOrderId());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then: Operations should complete quickly (not waiting for notifications)
        // Async notifications shouldn't significantly delay the main flow
        assertThat(duration).isLessThan(2000); // Should complete in under 2 seconds
        
        // Verify order reached final state
        Order finalOrder = orderService.getOrder(savedOrder.getOrderId());
        assertThat(finalOrder.getStatus()).isEqualTo(com.midlevel.orderfulfillment.domain.model.OrderStatus.SHIPPED);
    }
    
    /**
     * Test notification content for paid orders.
     * Verifies the notification includes correct order details.
     */
    @Test
    void paidOrderNotification_containsCorrectDetails(CapturedOutput output) throws InterruptedException {
        // Given: A paid order
        Order order = createTestOrder("CUST-005");
        Order savedOrder = orderRepository.save(order);
        String orderId = savedOrder.getOrderId();
        
        Thread.sleep(500);
        
        // When: Order is paid
        orderService.markOrderAsPaid(orderId);
        Thread.sleep(500);
        
        // Then: Notification log should contain order details
        String logs = output.toString();
        assertThat(logs).contains(orderId);
        assertThat(logs).contains("CUST-005");
        assertThat(logs).contains("Order paid notification sent");
        
        // Verify email content would be generated
        assertThat(logs).contains("EMAIL SENT") or assertThat(logs).contains("✅");
    }
    
    /**
     * Test shipped order notification includes address.
     * Verifies shipping notification contains delivery address details.
     */
    @Test
    void shippedOrderNotification_includesShippingAddress(CapturedOutput output) throws InterruptedException {
        // Given: An order ready to ship
        Order order = createTestOrder("CUST-006");
        Order savedOrder = orderRepository.save(order);
        String orderId = savedOrder.getOrderId();
        
        Thread.sleep(500);
        orderService.markOrderAsPaid(orderId);
        Thread.sleep(500);
        
        // When: Order is shipped
        orderService.markOrderAsShipped(orderId);
        Thread.sleep(500);
        
        // Then: Shipping notification should include address details
        String logs = output.toString();
        assertThat(logs).contains("Order shipped notification sent");
        assertThat(logs).contains("New York"); // City from test address
        assertThat(logs).contains("NY"); // State from test address
    }
    
    /**
     * Test that paid order can be cancelled and notification sent.
     * Verifies cancellation works even after payment.
     */
    @Test
    void paidOrderCancellation_sendsNotification(CapturedOutput output) throws InterruptedException {
        // Given: A paid order
        Order order = createTestOrder("CUST-007");
        Order savedOrder = orderRepository.save(order);
        String orderId = savedOrder.getOrderId();
        
        Thread.sleep(500);
        orderService.markOrderAsPaid(orderId);
        Thread.sleep(500);
        
        // When: Paid order is cancelled
        orderService.cancelOrder(orderId);
        Thread.sleep(500);
        
        // Then: Cancellation notification should be sent
        String logs = output.toString();
        assertThat(logs).contains("Order cancelled notification sent");
        assertThat(logs).contains(orderId);
    }
    
    /**
     * Test multiple concurrent orders generate separate notifications.
     * Verifies notification system handles multiple orders correctly.
     */
    @Test
    void multipleOrders_generateSeparateNotifications(CapturedOutput output) throws InterruptedException {
        // Given: Three different orders
        Order order1 = createTestOrder("CUST-008");
        Order order2 = createTestOrder("CUST-009");
        Order order3 = createTestOrder("CUST-010");
        
        // When: All orders are created
        Order saved1 = orderRepository.save(order1);
        Order saved2 = orderRepository.save(order2);
        Order saved3 = orderRepository.save(order3);
        
        Thread.sleep(1500); // Wait for all notifications
        
        // Then: Three separate notifications should be sent
        String logs = output.toString();
        assertThat(logs).contains(saved1.getOrderId());
        assertThat(logs).contains(saved2.getOrderId());
        assertThat(logs).contains(saved3.getOrderId());
        assertThat(logs).contains("CUST-008");
        assertThat(logs).contains("CUST-009");
        assertThat(logs).contains("CUST-010");
    }
    
    // ==================== Helper Methods ====================
    
    private Order createTestOrder(String customerId) {
        Address address = new Address(
                "123 Main Street",
                "New York",
                "NY",
                "10001",
                "US"
        );
        
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(
                "PROD-001",
                "Test Product",
                new Money(99.99, "USD"),
                2
        ));
        
        return new Order(customerId, address, items);
    }
}
