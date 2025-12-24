package com.midlevel.orderfulfillment.domain.model;

import java.util.Objects;

/**
 * OrderItem is a Value Object representing a single item in an order.
 * It contains the product information, quantity, and calculates the line total.
 * 
 * DDD Value Object: Immutable and equality by value.
 */
public final class OrderItem {
    
    // The unique identifier of the product (e.g., SKU)
    private final String productId;
    
    // The name/description of the product (for display purposes)
    private final String productName;
    
    // The price per unit
    private final Money unitPrice;
    
    // The quantity ordered (must be positive)
    private final int quantity;
    
    /**
     * Private constructor to enforce factory method pattern.
     * 
     * @param productId the product identifier
     * @param productName the product name
     * @param unitPrice the price per unit
     * @param quantity the quantity ordered
     */
    private OrderItem(String productId, String productName, Money unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
    
    /**
     * Factory method to create an OrderItem with full validation.
     * 
     * Business rules enforced:
     * - Product ID cannot be null or empty
     * - Product name cannot be null or empty
     * - Unit price cannot be null
     * - Quantity must be positive
     * 
     * @param productId the product identifier
     * @param productName the product name
     * @param unitPrice the price per unit
     * @param quantity the quantity ordered
     * @return a new OrderItem instance
     * @throws IllegalArgumentException if any validation fails
     */
    public static OrderItem of(String productId, String productName, Money unitPrice, int quantity) {
        // Validate product ID
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        
        // Validate product name
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        
        // Validate unit price
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
        
        // Validate quantity is positive (business rule: cannot order 0 or negative items)
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        
        // All validations passed - create the item
        return new OrderItem(productId, productName, unitPrice, quantity);
    }
    
    /**
     * Calculates the total price for this line item.
     * Formula: unitPrice * quantity
     * 
     * This is a derived value (computed from other fields) rather than stored state.
     * This follows the principle: "Don't store what you can compute."
     * 
     * @return the total price for this line item
     */
    public Money calculateLineTotal() {
        // Delegate to Money's multiply method
        return unitPrice.multiply(quantity);
    }
    
    /**
     * Creates a new OrderItem with a different quantity.
     * Since OrderItem is immutable, we create a new instance instead of modifying.
     * 
     * @param newQuantity the new quantity
     * @return a new OrderItem with updated quantity
     */
    public OrderItem withQuantity(int newQuantity) {
        // Use factory method to ensure validation
        return OrderItem.of(this.productId, this.productName, this.unitPrice, newQuantity);
    }
    
    // Getters (no setters - immutability)
    
    public String getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public Money getUnitPrice() {
        return unitPrice;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    /**
     * Value object equality: equal if all fields are equal.
     * Two OrderItems with same product, price, and quantity are considered equal.
     */
    @Override
    public boolean equals(Object o) {
        // Check reference equality (optimization)
        if (this == o) return true;
        
        // Check null and class type
        if (o == null || getClass() != o.getClass()) return false;
        
        // Cast and compare all fields
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
               Objects.equals(productId, orderItem.productId) &&
               Objects.equals(productName, orderItem.productName) &&
               Objects.equals(unitPrice, orderItem.unitPrice);
    }
    
    /**
     * Hash code consistent with equals.
     */
    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, unitPrice, quantity);
    }
    
    /**
     * Human-readable string representation.
     * Format: "ProductName (ID: xxx) - 3x @ $10.00 = $30.00"
     */
    @Override
    public String toString() {
        return String.format("%s (ID: %s) - %dx @ %s = %s",
            productName,
            productId,
            quantity,
            unitPrice,
            calculateLineTotal()
        );
    }
}
