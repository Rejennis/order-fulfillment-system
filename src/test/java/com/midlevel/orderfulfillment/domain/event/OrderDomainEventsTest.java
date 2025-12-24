package com.midlevel.orderfulfillment.domain.event;

import com.midlevel.orderfulfillment.domain.model.Address;
import com.midlevel.orderfulfillment.domain.model.Money;
import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for domain events being registered in Order aggregate.
 * 
 * These are unit tests - no Spring context needed.
 * We're just verifying that the domain model correctly registers events.
 */
@DisplayName("Order Domain Events Tests")
class OrderDomainEventsTest {
    
    @Test
    @DisplayName("Should register OrderCreatedEvent when order is created")
    void shouldRegisterOrderCreatedEvent() {
        // Given
        String customerId = "CUST-123";
        List<OrderItem> items = List.of(createOrderItem());
        Address address = createAddress();
        
        // When
        Order order = Order.create(customerId, items, address);
        
        // Then
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderCreatedEvent.class);
        
        OrderCreatedEvent event = (OrderCreatedEvent) order.getDomainEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getItemCount()).isEqualTo(1);
        assertThat(event.getTotalAmount()).isEqualTo(order.calculateTotal());
    }
    
    @Test
    @DisplayName("Should register OrderPaidEvent when order is paid")
    void shouldRegisterOrderPaidEvent() {
        // Given
        Order order = createOrder();
        order.clearDomainEvents();  // Clear creation event
        
        // When
        order.pay();
        
        // Then
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderPaidEvent.class);
        
        OrderPaidEvent event = (OrderPaidEvent) order.getDomainEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(event.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(event.getPaidAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should register OrderShippedEvent when order is shipped")
    void shouldRegisterOrderShippedEvent() {
        // Given
        Order order = createOrder();
        order.pay();
        order.clearDomainEvents();  // Clear previous events
        
        // When
        order.ship();
        
        // Then
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderShippedEvent.class);
        
        OrderShippedEvent event = (OrderShippedEvent) order.getDomainEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(event.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(event.getShippedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should register OrderCancelledEvent when order is cancelled")
    void shouldRegisterOrderCancelledEvent() {
        // Given
        Order order = createOrder();
        order.clearDomainEvents();  // Clear creation event
        
        // When
        order.cancel();
        
        // Then
        assertThat(order.getDomainEvents()).hasSize(1);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderCancelledEvent.class);
        
        OrderCancelledEvent event = (OrderCancelledEvent) order.getDomainEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(event.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(event.getReason()).isNotBlank();
    }
    
    @Test
    @DisplayName("Should clear domain events when requested")
    void shouldClearDomainEvents() {
        // Given
        Order order = createOrder();
        assertThat(order.getDomainEvents()).isNotEmpty();
        
        // When
        order.clearDomainEvents();
        
        // Then
        assertThat(order.getDomainEvents()).isEmpty();
    }
    
    @Test
    @DisplayName("Should accumulate multiple events")
    void shouldAccumulateMultipleEvents() {
        // Given
        Order order = createOrder();
        
        // When - perform multiple operations
        order.pay();
        order.ship();
        
        // Then - all events are accumulated
        assertThat(order.getDomainEvents()).hasSize(3);
        assertThat(order.getDomainEvents().get(0)).isInstanceOf(OrderCreatedEvent.class);
        assertThat(order.getDomainEvents().get(1)).isInstanceOf(OrderPaidEvent.class);
        assertThat(order.getDomainEvents().get(2)).isInstanceOf(OrderShippedEvent.class);
    }
    
    // Helper methods
    
    private Order createOrder() {
        return Order.create(
                "CUST-123",
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
}
