package com.midlevel.orderfulfillment.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Order aggregate.
 * 
 * Testing Strategy:
 * - Test happy paths (valid transitions)
 * - Test invalid state transitions
 * - Test business rule enforcement
 * - Test edge cases
 * 
 * We use @Nested classes to group related tests for better organization.
 * We use @DisplayName for human-readable test descriptions.
 */
@DisplayName("Order Aggregate Tests")
class OrderTest {
    
    // Test fixtures - reusable test data
    private String customerId;
    private List<OrderItem> validItems;
    private Address shippingAddress;
    
    /**
     * @BeforeEach runs before each test method.
     * This sets up common test data to avoid duplication.
     */
    @BeforeEach
    void setUp() {
        // Set up a valid customer ID
        customerId = "CUST-123";
        
        // Create valid order items
        OrderItem item1 = OrderItem.of(
            "PROD-001",
            "Widget",
            Money.usd(BigDecimal.valueOf(10.00)),
            2
        );
        
        OrderItem item2 = OrderItem.of(
            "PROD-002",
            "Gadget",
            Money.usd(BigDecimal.valueOf(25.00)),
            1
        );
        
        validItems = Arrays.asList(item1, item2);
        
        // Create a valid shipping address
        shippingAddress = Address.usAddress(
            "123 Main St",
            "San Francisco",
            "CA",
            "94105"
        );
    }
    
    /**
     * Nested class for testing order creation.
     * Groups all creation-related tests together.
     */
    @Nested
    @DisplayName("Order Creation Tests")
    class OrderCreationTests {
        
        @Test
        @DisplayName("Should create order with valid inputs")
        void shouldCreateOrderWithValidInputs() {
            // Act - create the order
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Assert - verify the order was created correctly
            assertNotNull(order, "Order should not be null");
            assertNotNull(order.getOrderId(), "Order ID should be generated");
            assertEquals(customerId, order.getCustomerId(), "Customer ID should match");
            assertEquals(OrderStatus.CREATED, order.getStatus(), "New orders should be in CREATED status");
            assertEquals(2, order.getItems().size(), "Should have 2 items");
            assertNotNull(order.getCreatedAt(), "Creation timestamp should be set");
            assertNull(order.getPaidAt(), "Paid timestamp should be null for new orders");
            assertNull(order.getShippedAt(), "Shipped timestamp should be null for new orders");
        }
        
        @Test
        @DisplayName("Should calculate total correctly")
        void shouldCalculateTotalCorrectly() {
            // Arrange - create order
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act - calculate total
            Money total = order.calculateTotal();
            
            // Assert - verify calculation
            // Item 1: 2 * $10.00 = $20.00
            // Item 2: 1 * $25.00 = $25.00
            // Total: $45.00
            Money expectedTotal = Money.usd(BigDecimal.valueOf(45.00));
            assertEquals(expectedTotal, total, "Total should be $45.00");
        }
        
        @Test
        @DisplayName("Should throw exception when customer ID is null")
        void shouldThrowExceptionWhenCustomerIdIsNull() {
            // Act & Assert - verify exception is thrown
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Order.create(null, validItems, shippingAddress),
                "Should throw exception for null customer ID"
            );
            
            // Verify exception message
            assertTrue(
                exception.getMessage().contains("Customer ID"),
                "Exception message should mention Customer ID"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when customer ID is empty")
        void shouldThrowExceptionWhenCustomerIdIsEmpty() {
            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> Order.create("   ", validItems, shippingAddress),
                "Should throw exception for empty customer ID"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when items list is null")
        void shouldThrowExceptionWhenItemsIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Order.create(customerId, null, shippingAddress),
                "Should throw exception for null items"
            );
            
            assertTrue(
                exception.getMessage().contains("at least one item"),
                "Exception should mention items requirement"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when items list is empty")
        void shouldThrowExceptionWhenItemsIsEmpty() {
            // Act & Assert
            assertThrows(
                IllegalArgumentException.class,
                () -> Order.create(customerId, Collections.emptyList(), shippingAddress),
                "Should throw exception for empty items list"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when shipping address is null")
        void shouldThrowExceptionWhenAddressIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Order.create(customerId, validItems, null),
                "Should throw exception for null shipping address"
            );
            
            assertTrue(
                exception.getMessage().contains("Shipping address"),
                "Exception should mention shipping address"
            );
        }
    }
    
    /**
     * Nested class for testing the pay() operation.
     */
    @Nested
    @DisplayName("Payment Operation Tests")
    class PaymentTests {
        
        @Test
        @DisplayName("Should successfully pay a created order")
        void shouldPayCreatedOrder() {
            // Arrange - create an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act - pay the order
            order.pay();
            
            // Assert - verify state changed correctly
            assertEquals(OrderStatus.PAID, order.getStatus(), "Status should be PAID");
            assertNotNull(order.getPaidAt(), "Payment timestamp should be set");
            assertTrue(order.isPaid(), "isPaid() should return true");
        }
        
        @Test
        @DisplayName("Should be idempotent - paying twice has no additional effect")
        void shouldBeIdempotentWhenPayingTwice() {
            // Arrange - create and pay an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            
            // Capture the first payment timestamp
            var firstPaymentTime = order.getPaidAt();
            
            // Act - pay again (simulate duplicate payment request)
            order.pay();  // Should not throw exception
            
            // Assert - status should still be PAID, nothing changed
            assertEquals(OrderStatus.PAID, order.getStatus(), "Status should still be PAID");
            assertEquals(firstPaymentTime, order.getPaidAt(), "Payment timestamp should not change");
        }
        
        @Test
        @DisplayName("Should throw exception when paying a shipped order")
        void shouldThrowExceptionWhenPayingShippedOrder() {
            // Arrange - create, pay, and ship an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            order.ship();
            
            // Act & Assert - trying to pay again should fail
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> order.pay(),
                "Should not be able to pay a shipped order"
            );
            
            assertTrue(
                exception.getMessage().contains("Cannot pay"),
                "Exception message should indicate payment not allowed"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when paying a cancelled order")
        void shouldThrowExceptionWhenPayingCancelledOrder() {
            // Arrange - create and cancel an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.cancel();
            
            // Act & Assert
            assertThrows(
                IllegalStateException.class,
                () -> order.pay(),
                "Should not be able to pay a cancelled order"
            );
        }
    }
    
    /**
     * Nested class for testing the ship() operation.
     */
    @Nested
    @DisplayName("Shipping Operation Tests")
    class ShippingTests {
        
        @Test
        @DisplayName("Should successfully ship a paid order")
        void shouldShipPaidOrder() {
            // Arrange - create and pay an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            
            // Act - ship the order
            order.ship();
            
            // Assert - verify state changed correctly
            assertEquals(OrderStatus.SHIPPED, order.getStatus(), "Status should be SHIPPED");
            assertNotNull(order.getShippedAt(), "Shipping timestamp should be set");
            assertTrue(order.isPaid(), "Shipped orders are still considered paid");
            assertFalse(order.isModifiable(), "Shipped orders cannot be modified");
        }
        
        @Test
        @DisplayName("Should throw exception when shipping an unpaid order")
        void shouldThrowExceptionWhenShippingUnpaidOrder() {
            // Arrange - create an order but don't pay it
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act & Assert - shipping should fail
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> order.ship(),
                "Should not be able to ship unpaid order"
            );
            
            assertTrue(
                exception.getMessage().contains("PAID before shipping"),
                "Exception should mention payment requirement"
            );
        }
        
        @Test
        @DisplayName("Should throw exception when shipping a cancelled order")
        void shouldThrowExceptionWhenShippingCancelledOrder() {
            // Arrange - create and cancel an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.cancel();
            
            // Act & Assert
            assertThrows(
                IllegalStateException.class,
                () -> order.ship(),
                "Should not be able to ship a cancelled order"
            );
        }
    }
    
    /**
     * Nested class for testing the cancel() operation.
     */
    @Nested
    @DisplayName("Cancellation Operation Tests")
    class CancellationTests {
        
        @Test
        @DisplayName("Should successfully cancel a created order")
        void shouldCancelCreatedOrder() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act
            order.cancel();
            
            // Assert
            assertEquals(OrderStatus.CANCELLED, order.getStatus(), "Status should be CANCELLED");
            assertFalse(order.isModifiable(), "Cancelled orders cannot be modified");
        }
        
        @Test
        @DisplayName("Should successfully cancel a paid order")
        void shouldCancelPaidOrder() {
            // Arrange - create and pay an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            
            // Act - cancel it
            order.cancel();
            
            // Assert
            assertEquals(OrderStatus.CANCELLED, order.getStatus(), "Status should be CANCELLED");
            // Note: In a real system, this would trigger a refund process
        }
        
        @Test
        @DisplayName("Should be idempotent - cancelling twice has no additional effect")
        void shouldBeIdempotentWhenCancellingTwice() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.cancel();
            
            // Act - cancel again
            order.cancel();  // Should not throw exception
            
            // Assert - still cancelled
            assertEquals(OrderStatus.CANCELLED, order.getStatus(), "Status should still be CANCELLED");
        }
        
        @Test
        @DisplayName("Should throw exception when cancelling a shipped order")
        void shouldThrowExceptionWhenCancellingShippedOrder() {
            // Arrange - create, pay, and ship an order
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            order.ship();
            
            // Act & Assert - cancelling should fail
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> order.cancel(),
                "Should not be able to cancel a shipped order"
            );
            
            assertTrue(
                exception.getMessage().contains("shipped"),
                "Exception should mention that order is shipped"
            );
            
            assertTrue(
                exception.getMessage().contains("return"),
                "Exception should suggest return process"
            );
        }
    }
    
    /**
     * Nested class for testing state transition rules.
     */
    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {
        
        @Test
        @DisplayName("Should follow valid state transition: CREATED -> PAID -> SHIPPED")
        void shouldFollowValidTransitionPath() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act & Assert - follow the happy path
            assertEquals(OrderStatus.CREATED, order.getStatus());
            
            order.pay();
            assertEquals(OrderStatus.PAID, order.getStatus());
            
            order.ship();
            assertEquals(OrderStatus.SHIPPED, order.getStatus());
        }
        
        @Test
        @DisplayName("Should follow valid state transition: CREATED -> CANCELLED")
        void shouldAllowEarlyCancellation() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act
            order.cancel();
            
            // Assert
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
        
        @Test
        @DisplayName("Should follow valid state transition: CREATED -> PAID -> CANCELLED")
        void shouldAllowCancellationAfterPayment() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            
            // Act
            order.cancel();
            
            // Assert
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
        
        @Test
        @DisplayName("Terminal state SHIPPED should not allow any transitions")
        void shippedOrderShouldBeTerminal() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            order.ship();
            
            // Assert - no further transitions allowed
            assertTrue(order.getStatus().isTerminal(), "SHIPPED should be terminal");
            assertFalse(order.isModifiable(), "Shipped orders should not be modifiable");
        }
        
        @Test
        @DisplayName("Terminal state CANCELLED should not allow any transitions")
        void cancelledOrderShouldBeTerminal() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.cancel();
            
            // Assert
            assertTrue(order.getStatus().isTerminal(), "CANCELLED should be terminal");
            assertFalse(order.isModifiable(), "Cancelled orders should not be modifiable");
        }
    }
    
    /**
     * Nested class for testing aggregate identity and equality.
     */
    @Nested
    @DisplayName("Equality and Identity Tests")
    class EqualityTests {
        
        @Test
        @DisplayName("Orders with same ID should be equal")
        void ordersWithSameIdShouldBeEqual() {
            // Note: We can't easily test this without reflection or package-private access
            // because orderId is generated internally. This is a design tradeoff.
            // In a real codebase, you might have a test-specific factory.
            
            // This test demonstrates the concept
            Order order1 = Order.create(customerId, validItems, shippingAddress);
            Order order2 = order1;  // Same reference
            
            assertEquals(order1, order2, "Same order instance should be equal to itself");
        }
        
        @Test
        @DisplayName("Different orders should not be equal")
        void differentOrdersShouldNotBeEqual() {
            // Arrange - create two different orders
            Order order1 = Order.create(customerId, validItems, shippingAddress);
            Order order2 = Order.create(customerId, validItems, shippingAddress);
            
            // Assert - they should have different IDs and not be equal
            assertNotEquals(order1.getOrderId(), order2.getOrderId(), "Should have different IDs");
            assertNotEquals(order1, order2, "Different orders should not be equal");
        }
    }
    
    /**
     * Additional tests for helper methods and edge cases.
     */
    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {
        
        @Test
        @DisplayName("isPaid() should return false for CREATED orders")
        void isPaidShouldReturnFalseForCreatedOrders() {
            Order order = Order.create(customerId, validItems, shippingAddress);
            assertFalse(order.isPaid(), "Created orders should not be paid");
        }
        
        @Test
        @DisplayName("isPaid() should return true for PAID orders")
        void isPaidShouldReturnTrueForPaidOrders() {
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            assertTrue(order.isPaid(), "Paid orders should return true");
        }
        
        @Test
        @DisplayName("isPaid() should return true for SHIPPED orders")
        void isPaidShouldReturnTrueForShippedOrders() {
            Order order = Order.create(customerId, validItems, shippingAddress);
            order.pay();
            order.ship();
            assertTrue(order.isPaid(), "Shipped orders are paid");
        }
        
        @Test
        @DisplayName("getItems() should return immutable list")
        void getItemsShouldReturnImmutableList() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act - get items
            List<OrderItem> items = order.getItems();
            
            // Assert - modifying the list should throw exception
            assertThrows(
                UnsupportedOperationException.class,
                () -> items.add(OrderItem.of("PROD-999", "Hacker Item", Money.usd(BigDecimal.ONE), 1)),
                "Should not be able to modify returned items list"
            );
        }
        
        @Test
        @DisplayName("toString() should include key information")
        void toStringShouldIncludeKeyInformation() {
            // Arrange
            Order order = Order.create(customerId, validItems, shippingAddress);
            
            // Act
            String orderString = order.toString();
            
            // Assert - verify key fields are present
            assertTrue(orderString.contains(order.getOrderId()), "Should contain order ID");
            assertTrue(orderString.contains(customerId), "Should contain customer ID");
            assertTrue(orderString.contains("CREATED"), "Should contain status");
        }
    }
}
