package com.midlevel.orderfulfillment.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for creating a new order via REST API.
 * 
 * Why DTOs (Data Transfer Objects)?
 * 1. API Stability: Domain model can change without breaking API contracts
 * 2. Validation: Add API-specific validation rules
 * 3. Security: Control what data is exposed/accepted
 * 4. Flexibility: Different API versions can use different DTOs with same domain
 * 
 * Annotations:
 * - @NotNull: Field cannot be null
 * - @NotBlank: String cannot be null, empty, or whitespace-only
 * - @NotEmpty: Collection cannot be null or empty
 * - @Valid: Validate nested objects
 */
public record CreateOrderRequest(
        
        @NotBlank(message = "Customer ID is required")
        String customerId,
        
        @NotNull(message = "Shipping address is required")
        @Valid
        AddressDto shippingAddress,
        
        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items
) {
    
    /**
     * DTO for order item within create order request.
     */
    public record OrderItemRequest(
            @NotBlank(message = "Product ID is required")
            String productId,
            
            @NotBlank(message = "Product name is required")
            String productName,
            
            @NotNull(message = "Unit price is required")
            @Valid
            MoneyDto unitPrice,
            
            @NotNull(message = "Quantity is required")
            Integer quantity
    ) {}
}
