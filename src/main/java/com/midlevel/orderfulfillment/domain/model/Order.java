package com.midlevel.orderfulfillment.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order is the main Aggregate Root in our domain.
 * 
 * DDD Aggregate Root responsibilities:
 * - Enforces business invariants (rules that must always be true)
 * - Controls state transitions via methods (not direct field access)
 * - Ensures consistency within the aggregate boundary
 * - Is the only entry point for modifying the aggregate
 * 
 * Business Rules enforced by this aggregate:
 * 1. Orders must have at least one item
 * 2. Order total must be greater than zero
 * 3. State transitions must follow the state machine rules
 * 4. Cannot ship an unpaid order
 * 5. Cannot cancel a shipped order
 * 6. Payment operations are idempotent (can't pay twice)
 */
public class Order {
    
    // Unique identifier for this order (immutable - set once at creation)
    private final String orderId;
    
    // Customer who placed the order (immutable - doesn't change after creation)
    private final String customerId;
    
    // When the order was created (immutable - set once)
    private final Instant createdAt;
    
    // List of items in the order (mutable internally, but exposed as immutable)
    private final List<OrderItem> items;
    
    // Shipping address (immutable - set at creation)
    private final Address shippingAddress;
    
    // Current status of the order (mutable - changes through state transitions)
    private OrderStatus status;
    
    // When the order was paid (null until payment)
    private Instant paidAt;
    
    // When the order was shipped (null until shipping)
    private Instant shippedAt;
    
    /**
     * Private constructor to enforce factory method pattern.
     * This ensures all Order instances go through proper validation.
     */
    private Order(String orderId, String customerId, List<OrderItem> items, Address shippingAddress) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);  // Defensive copy to prevent external modification
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.CREATED;     // New orders always start in CREATED state
        this.createdAt = Instant.now();        // Capture creation timestamp
        this.paidAt = null;                     // Not paid yet
        this.shippedAt = null;                  // Not shipped yet
    }
    
    /**
     * Factory method to create a new Order.
     * This is the only way to create an Order from outside the class.
     * 
     * @param customerId the customer placing the order
     * @param items the items being ordered
     * @param shippingAddress where to ship the order
     * @return a new Order in CREATED status
     * @throws IllegalArgumentException if validation fails
     */
    public static Order create(String customerId, List<OrderItem> items, Address shippingAddress) {
        // Generate a unique order ID using UUID
        String orderId = UUID.randomUUID().toString();
        
        // Validate customer ID
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        // Validate items list is not null or empty (Business Rule #1)
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        
        // Validate shipping address
        if (shippingAddress == null) {
            throw new IllegalArgumentException("Shipping address cannot be null");
        }
        
        // Create the order instance
        Order order = new Order(orderId, customerId, items, shippingAddress);
        
        // Validate the total is greater than zero (Business Rule #2)
        if (order.calculateTotal().isZero()) {
            throw new IllegalArgumentException("Order total must be greater than zero");
        }
        
        // Return the validated order
        return order;
    }
    
    /**
     * Calculates the total amount for this order.
     * This is a derived value computed by summing all line item totals.
     * 
     * @return the total order amount
     */
    public Money calculateTotal() {
        // Start with the first item's total
        Money total = items.get(0).calculateLineTotal();
        
        // Add each subsequent item's total
        for (int i = 1; i < items.size(); i++) {
            total = total.add(items.get(i).calculateLineTotal());
        }
        
        return total;
    }
    
    /**
     * Transitions the order to PAID status.
     * 
     * Business Rules:
     * - Can only pay orders in CREATED or PAID status
     * - Payment is idempotent (calling multiple times has no additional effect)
     * 
     * This method demonstrates the Command pattern in DDD:
     * - Named after the business action (pay, not setPaid)
     * - Enforces business rules before changing state
     * - Returns void or throws exception (no return value)
     * 
     * @throws IllegalStateException if order cannot be paid from current status
     */
    public void pay() {
        // Idempotent check: If already paid, do nothing (Business Rule #6)
        if (this.status == OrderStatus.PAID) {
            // Already paid - this is a duplicate payment request
            // In a real system, we might log this or return a payment ID
            return;
        }
        
        // Validate state transition is allowed (Business Rule #3)
        if (!this.status.canTransitionTo(OrderStatus.PAID)) {
            throw new IllegalStateException(
                "Cannot pay order in status: " + this.status + 
                ". Order must be in CREATED status."
            );
        }
        
        // Transition to PAID status
        this.status = OrderStatus.PAID;
        
        // Record payment timestamp
        this.paidAt = Instant.now();
        
        // In a real system, we would emit a OrderPaidEvent here
        // This would trigger notifications, inventory updates, etc.
    }
    
    /**
     * Transitions the order to SHIPPED status.
     * 
     * Business Rules:
     * - Can only ship orders in PAID status (Business Rule #4)
     * - Shipping is the point of no return (can't cancel after)
     * 
     * @throws IllegalStateException if order cannot be shipped from current status
     */
    public void ship() {
        // Validate current status allows shipping (Business Rule #4)
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException(
                "Cannot ship order in status: " + this.status + 
                ". Order must be PAID before shipping."
            );
        }
        
        // Validate state transition is allowed
        if (!this.status.canTransitionTo(OrderStatus.SHIPPED)) {
            throw new IllegalStateException(
                "Cannot transition from " + this.status + " to SHIPPED"
            );
        }
        
        // Transition to SHIPPED status
        this.status = OrderStatus.SHIPPED;
        
        // Record shipping timestamp
        this.shippedAt = Instant.now();
        
        // In a real system, we would:
        // - Emit OrderShippedEvent
        // - Update inventory
        // - Send shipping confirmation to customer
    }
    
    /**
     * Transitions the order to CANCELLED status.
     * 
     * Business Rules:
     * - Can cancel orders in CREATED or PAID status
     * - Cannot cancel shipped orders (Business Rule #5)
     * 
     * @throws IllegalStateException if order cannot be cancelled from current status
     */
    public void cancel() {
        // Check if we're in a terminal state (Business Rule #5)
        if (this.status == OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                "Cannot cancel order that has been shipped. " +
                "Consider processing a return instead."
            );
        }
        
        // Idempotent check: If already cancelled, do nothing
        if (this.status == OrderStatus.CANCELLED) {
            return;  // Already cancelled
        }
        
        // Validate state transition is allowed
        if (!this.status.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException(
                "Cannot cancel order in status: " + this.status
            );
        }
        
        // Transition to CANCELLED status
        this.status = OrderStatus.CANCELLED;
        
        // In a real system, we would:
        // - Emit OrderCancelledEvent
        // - Process refund if already paid
        // - Release inventory back to stock
        // - Notify customer
    }
    
    /**
     * Checks if the order has been paid.
     * Convenience method for common business logic checks.
     * 
     * @return true if order is in PAID or SHIPPED status
     */
    public boolean isPaid() {
        return this.status == OrderStatus.PAID || this.status == OrderStatus.SHIPPED;
    }
    
    /**
     * Checks if the order can be modified.
     * Orders in terminal states (SHIPPED, CANCELLED) cannot be modified.
     * 
     * @return true if order is still modifiable
     */
    public boolean isModifiable() {
        return !this.status.isTerminal();
    }
    
    // Getters (no setters - state changes only through domain methods)
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getPaidAt() {
        return paidAt;
    }
    
    public Instant getShippedAt() {
        return shippedAt;
    }
    
    /**
     * Returns an immutable view of the order items.
     * This prevents external code from modifying the internal list.
     * 
     * Defensive programming: Never expose mutable collections directly.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public Address getShippingAddress() {
        return shippingAddress;  // Address is already immutable (value object)
    }
    
    /**
     * Aggregate equality is based on identity (orderId), not value.
     * Two orders with the same ID are the same order, even if other fields differ.
     * 
     * This is different from Value Objects which use value equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Order order = (Order) o;
        // Only compare orderId (identity equality for aggregates)
        return Objects.equals(orderId, order.orderId);
    }
    
    /**
     * Hash code based on identity (orderId).
     */
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    /**
     * Human-readable string representation.
     * Useful for logging and debugging.
     */
    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status=" + status +
                ", itemCount=" + items.size() +
                ", total=" + calculateTotal() +
                ", createdAt=" + createdAt +
                '}';
    }
}
