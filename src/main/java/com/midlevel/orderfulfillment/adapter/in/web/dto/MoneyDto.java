package com.midlevel.orderfulfillment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO for money/currency information.
 * Used in both requests and responses.
 */
public record MoneyDto(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,
        
        @NotBlank(message = "Currency is required")
        String currency
) {}
