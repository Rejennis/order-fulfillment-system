package com.midlevel.orderfulfillment.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for order response sent to API clients.
 * 
 * Design decisions:
 * - Immutable record for thread safety and clarity
 * - Contains all data needed by clients
 * - Timestamps as ISO-8601 strings (automatic with Spring Boot)
 * - Nested DTOs for items and address
 */
public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        Instant createdAt,
        Instant paidAt,
        Instant shippedAt,
        AddressDto shippingAddress,
        List<OrderItemResponse> items,
        MoneyDto totalAmount
) {
    
    /**
     * DTO for order item in response.
     */
    public record OrderItemResponse(
            String productId,
            String productName,
            MoneyDto unitPrice,
            Integer quantity,
            MoneyDto lineTotal
    ) {}
}
