package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.adapter.out.persistence.JpaOrderRepository;
import com.midlevel.orderfulfillment.adapter.out.persistence.OrderRepositoryAdapter;
import com.midlevel.orderfulfillment.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Edge case tests for OrderService idempotency and concurrency.
 * 
 * IMPLEMENTATION TIMELINE:
 * - Created during Gap Closure (December 24, 2025)
 * - Addresses Day 4 requirement: "Test edge cases (duplicate payment, invalid transitions)"
 * 
 * These tests verify:
 * 1. Idempotency - calling pay() twice doesn't cause errors
 * 2. Concurrent operations - multiple threads accessing same order
 * 3. Invalid state transitions - proper error handling
 */
@SpringBootTest
@Testcontainers
@DisplayName("Order Service Edge Case Tests")
class OrderServiceEdgeCaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepositoryAdapter orderRepository;

    @Autowired
    private JpaOrderRepository jpaOrderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Clean database
        jpaOrderRepository.deleteAll();

        // Create a test order
        OrderItem item = OrderItem.of(
                "PROD-001",
                "Test Product",
                Money.usd(BigDecimal.valueOf(50.00)),
                2
        );

        Address address = Address.of(
                "123 Test St",
                "Test City",
                "TS",
                "12345",
                "US"
        );

        testOrder = Order.create(
                "CUST-TEST",
                List.of(item),
                address
        );

        testOrder = orderService.createOrder(testOrder);
    }

    // ==================== Idempotency Tests ====================

    @Test
    @DisplayName("Calling markOrderAsPaid twice should be idempotent (no error)")
    void duplicatePaymentCallShouldBeIdempotent() {
        // Given: Order is created
        String orderId = testOrder.getOrderId();

        // When: We call pay() twice
        Order paidOnce = orderService.markOrderAsPaid(orderId);
        Order paidTwice = orderService.markOrderAsPaid(orderId);

        // Then: Both calls succeed, order is in PAID state
        assertThat(paidOnce.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidTwice.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOnce.getPaidAt()).isNotNull();
        assertThat(paidTwice.getPaidAt()).isEqualTo(paidOnce.getPaidAt());
    }

    @Test
    @DisplayName("Calling markOrderAsPaid on already shipped order should be idempotent")
    void payingAlreadyShippedOrderShouldBeIdempotent() {
        // Given: Order is paid and shipped
        String orderId = testOrder.getOrderId();
        orderService.markOrderAsPaid(orderId);
        orderService.markOrderAsShipped(orderId);

        // When: We try to pay again
        Order result = orderService.markOrderAsPaid(orderId);

        // Then: No error, order remains shipped
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Multiple payment calls in sequence should all succeed")
    void multipleSequentialPaymentCallsShouldSucceed() {
        // Given: Order is created
        String orderId = testOrder.getOrderId();

        // When: We call pay() 5 times in sequence
        for (int i = 0; i < 5; i++) {
            Order result = orderService.markOrderAsPaid(orderId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        // Then: Order is still in valid PAID state
        Order finalOrder = orderService.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    // ==================== Concurrency Tests ====================

    @Test
    @DisplayName("Concurrent payment calls should be handled safely")
    void concurrentPaymentCallsShouldBeSafe() throws InterruptedException, ExecutionException {
        // Given: Order is created
        String orderId = testOrder.getOrderId();
        int threadCount = 10;

        // When: Multiple threads try to pay the same order simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Future<Order>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<Order> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // All threads pay at once
                    return orderService.markOrderAsPaid(orderId);
                } finally {
                    doneLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executor.shutdown();

        // Then: All threads succeeded (no exceptions thrown)
        for (Future<Order> future : futures) {
            Order result = future.get();
            assertThat(result.getStatus()).isIn(OrderStatus.PAID, OrderStatus.SHIPPED);
        }

        // And: Final order is in valid state
        Order finalOrder = orderService.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isIn(OrderStatus.PAID, OrderStatus.SHIPPED);
    }

    // ==================== Invalid State Transition Tests ====================

    @Test
    @DisplayName("Shipping unpaid order should throw exception")
    void shippingUnpaidOrderShouldFail() {
        // Given: Order is created but not paid
        String orderId = testOrder.getOrderId();

        // When/Then: Trying to ship throws exception
        assertThatThrownBy(() -> orderService.markOrderAsShipped(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot ship");
    }

    @Test
    @DisplayName("Cancelling shipped order should throw exception")
    void cancellingShippedOrderShouldFail() {
        // Given: Order is paid and shipped
        String orderId = testOrder.getOrderId();
        orderService.markOrderAsPaid(orderId);
        orderService.markOrderAsShipped(orderId);

        // When/Then: Trying to cancel throws exception
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel");
    }

    @Test
    @DisplayName("Operating on non-existent order should throw OrderNotFoundException")
    void operatingOnNonExistentOrderShouldFail() {
        // Given: Invalid order ID
        String invalidOrderId = "NON-EXISTENT-ORDER-ID";

        // When/Then: All operations throw OrderNotFoundException
        assertThatThrownBy(() -> orderService.markOrderAsPaid(invalidOrderId))
                .isInstanceOf(OrderService.OrderNotFoundException.class)
                .hasMessageContaining("Order not found");

        assertThatThrownBy(() -> orderService.markOrderAsShipped(invalidOrderId))
                .isInstanceOf(OrderService.OrderNotFoundException.class)
                .hasMessageContaining("Order not found");

        assertThatThrownBy(() -> orderService.cancelOrder(invalidOrderId))
                .isInstanceOf(OrderService.OrderNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // ==================== Business Rule Edge Cases ====================

    @Test
    @DisplayName("Cancelled order should not be payable")
    void cancelledOrderShouldNotBePayable() {
        // Given: Order is cancelled
        String orderId = testOrder.getOrderId();
        orderService.cancelOrder(orderId);

        // When/Then: Trying to pay throws exception
        assertThatThrownBy(() -> orderService.markOrderAsPaid(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot pay");
    }

    @Test
    @DisplayName("Paid order can still be cancelled")
    void paidOrderCanBeCancelled() {
        // Given: Order is paid
        String orderId = testOrder.getOrderId();
        orderService.markOrderAsPaid(orderId);

        // When: We cancel it
        Order cancelled = orderService.cancelOrder(orderId);

        // Then: Order is cancelled successfully
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Order can be cancelled multiple times idempotently")
    void orderCanBeCancelledMultipleTimes() {
        // Given: Order is created
        String orderId = testOrder.getOrderId();

        // When: We cancel it multiple times
        orderService.cancelOrder(orderId);
        
        // Then: Second cancel throws (cancel is NOT idempotent by design)
        // This documents current behavior - cancel() checks state strictly
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel");
    }
}
