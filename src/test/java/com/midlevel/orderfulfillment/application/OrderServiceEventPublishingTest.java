package com.midlevel.orderfulfillment.application;

import com.midlevel.orderfulfillment.domain.event.OrderCreatedEvent;
import com.midlevel.orderfulfillment.domain.event.OrderPaidEvent;
import com.midlevel.orderfulfillment.domain.event.OrderShippedEvent;
import com.midlevel.orderfulfillment.domain.event.OrderCancelledEvent;
import com.midlevel.orderfulfillment.domain.model.Address;
import com.midlevel.orderfulfillment.domain.model.Money;
import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for domain event publishing.
 * 
 * This tests the full event flow:
 * 1. Domain registers events
 * 2. Service publishes events
 * 3. Event listeners receive events
 * 
 * Uses a test event listener to capture events for verification.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Order Service Event Publishing Integration Tests")
class OrderServiceEventPublishingTest {
    
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
    private TestEventListener testEventListener;
    
    @Test
    @DisplayName("Should publish OrderCreatedEvent when order is created")
    void shouldPublishOrderCreatedEvent() throws InterruptedException {
        // Given
        Order order = createOrder();
        
        // When
        orderService.createOrder(order);
        
        // Then - wait for async event processing
        boolean eventReceived = testEventListener.awaitOrderCreatedEvent(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.getOrderCreatedEvents()).hasSize(1);
        
        OrderCreatedEvent event = testEventListener.getOrderCreatedEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(event.getCustomerId()).isEqualTo(order.getCustomerId());
    }
    
    @Test
    @DisplayName("Should publish OrderPaidEvent when order is paid")
    void shouldPublishOrderPaidEvent() throws InterruptedException {
        // Given
        Order order = createOrder();
        Order savedOrder = orderService.createOrder(order);
        testEventListener.reset();
        
        // When
        orderService.markOrderAsPaid(savedOrder.getOrderId());
        
        // Then
        boolean eventReceived = testEventListener.awaitOrderPaidEvent(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.getOrderPaidEvents()).hasSize(1);
        
        OrderPaidEvent event = testEventListener.getOrderPaidEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(savedOrder.getOrderId());
    }
    
    @Test
    @DisplayName("Should publish OrderShippedEvent when order is shipped")
    void shouldPublishOrderShippedEvent() throws InterruptedException {
        // Given
        Order order = createOrder();
        Order savedOrder = orderService.createOrder(order);
        orderService.markOrderAsPaid(savedOrder.getOrderId());
        testEventListener.reset();
        
        // When
        orderService.markOrderAsShipped(savedOrder.getOrderId());
        
        // Then
        boolean eventReceived = testEventListener.awaitOrderShippedEvent(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.getOrderShippedEvents()).hasSize(1);
        
        OrderShippedEvent event = testEventListener.getOrderShippedEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(savedOrder.getOrderId());
    }
    
    @Test
    @DisplayName("Should publish OrderCancelledEvent when order is cancelled")
    void shouldPublishOrderCancelledEvent() throws InterruptedException {
        // Given
        Order order = createOrder();
        Order savedOrder = orderService.createOrder(order);
        testEventListener.reset();
        
        // When
        orderService.cancelOrder(savedOrder.getOrderId());
        
        // Then
        boolean eventReceived = testEventListener.awaitOrderCancelledEvent(2, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.getOrderCancelledEvents()).hasSize(1);
        
        OrderCancelledEvent event = testEventListener.getOrderCancelledEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(savedOrder.getOrderId());
    }
    
    // Helper methods
    
    private Order createOrder() {
        return Order.create(
                "CUST-" + System.currentTimeMillis(),
                List.of(createOrderItem()),
                createAddress()
        );
    }
    
    private OrderItem createOrderItem() {
        return OrderItem.create(
                "PROD-001",
                "Test Product",
                new Money(new BigDecimal("99.99"), "USD"),
                1
        );
    }
    
    private Address createAddress() {
        return new Address(
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "US"
        );
    }
    
    /**
     * Test event listener to capture events for verification.
     * This is registered as a Spring bean and listens to the same events.
     */
    @TestConfiguration
    static class TestEventListenerConfig {
        // TestEventListener is auto-registered by @Component
    }
    
    @Component
    static class TestEventListener {
        
        private final List<OrderCreatedEvent> orderCreatedEvents = new ArrayList<>();
        private final List<OrderPaidEvent> orderPaidEvents = new ArrayList<>();
        private final List<OrderShippedEvent> orderShippedEvents = new ArrayList<>();
        private final List<OrderCancelledEvent> orderCancelledEvents = new ArrayList<>();
        
        private CountDownLatch orderCreatedLatch = new CountDownLatch(1);
        private CountDownLatch orderPaidLatch = new CountDownLatch(1);
        private CountDownLatch orderShippedLatch = new CountDownLatch(1);
        private CountDownLatch orderCancelledLatch = new CountDownLatch(1);
        
        @EventListener
        public void handleOrderCreated(OrderCreatedEvent event) {
            orderCreatedEvents.add(event);
            orderCreatedLatch.countDown();
        }
        
        @EventListener
        public void handleOrderPaid(OrderPaidEvent event) {
            orderPaidEvents.add(event);
            orderPaidLatch.countDown();
        }
        
        @EventListener
        public void handleOrderShipped(OrderShippedEvent event) {
            orderShippedEvents.add(event);
            orderShippedLatch.countDown();
        }
        
        @EventListener
        public void handleOrderCancelled(OrderCancelledEvent event) {
            orderCancelledEvents.add(event);
            orderCancelledLatch.countDown();
        }
        
        public boolean awaitOrderCreatedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderCreatedLatch.await(timeout, unit);
        }
        
        public boolean awaitOrderPaidEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderPaidLatch.await(timeout, unit);
        }
        
        public boolean awaitOrderShippedEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderShippedLatch.await(timeout, unit);
        }
        
        public boolean awaitOrderCancelledEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return orderCancelledLatch.await(timeout, unit);
        }
        
        public List<OrderCreatedEvent> getOrderCreatedEvents() {
            return orderCreatedEvents;
        }
        
        public List<OrderPaidEvent> getOrderPaidEvents() {
            return orderPaidEvents;
        }
        
        public List<OrderShippedEvent> getOrderShippedEvents() {
            return orderShippedEvents;
        }
        
        public List<OrderCancelledEvent> getOrderCancelledEvents() {
            return orderCancelledEvents;
        }
        
        public void reset() {
            orderCreatedEvents.clear();
            orderPaidEvents.clear();
            orderShippedEvents.clear();
            orderCancelledEvents.clear();
            orderCreatedLatch = new CountDownLatch(1);
            orderPaidLatch = new CountDownLatch(1);
            orderShippedLatch = new CountDownLatch(1);
            orderCancelledLatch = new CountDownLatch(1);
        }
    }
}
