package com.midlevel.orderfulfillment.domain.model;

/**
 * OrderStatus represents the possible states an Order can be in during its lifecycle.
 * This enum is part of the domain layer and enforces valid state transitions.
 * 
 * DDD Principle: Use enums for finite states to make illegal states unrepresentable.
 */
public enum OrderStatus {
    
    /**
     * CREATED: Initial state when an order is first placed.
     * From here, order can be PAID or CANCELLED.
     */
    CREATED,
    
    /**
     * PAID: Order has been successfully paid.
     * From here, order can be SHIPPED or CANCELLED.
     */
    PAID,
    
    /**
     * SHIPPED: Order has been shipped to customer.
     * This is a terminal state - cannot transition from here.
     */
    SHIPPED,
    
    /**
     * CANCELLED: Order has been cancelled.
     * This is a terminal state - cannot transition from here.
     */
    CANCELLED;
    
    /**
     * Checks if the current status can transition to the target status.
     * This method encapsulates the business rules for valid state transitions.
     * 
     * Valid transitions:
     * - CREATED -> PAID
     * - CREATED -> CANCELLED
     * - PAID -> SHIPPED
     * - PAID -> CANCELLED
     * 
     * @param targetStatus the status we want to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(OrderStatus targetStatus) {
        // Switch on the current status to determine valid next states
        switch (this) {
            case CREATED:
                // From CREATED, we can go to PAID or CANCELLED
                return targetStatus == PAID || targetStatus == CANCELLED;
                
            case PAID:
                // From PAID, we can go to SHIPPED or CANCELLED
                return targetStatus == SHIPPED || targetStatus == CANCELLED;
                
            case SHIPPED:
            case CANCELLED:
                // Terminal states - no transitions allowed
                return false;
                
            default:
                // Default case for safety (should never reach here)
                return false;
        }
    }
    
    /**
     * Checks if this status represents a terminal state.
     * Terminal states are final and cannot transition to other states.
     * 
     * @return true if this is a terminal state (SHIPPED or CANCELLED)
     */
    public boolean isTerminal() {
        // Only SHIPPED and CANCELLED are terminal states
        return this == SHIPPED || this == CANCELLED;
    }
}
