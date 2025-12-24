package com.midlevel.orderfulfillment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for address information.
 * Used in both requests and responses.
 */
public record AddressDto(
        @NotBlank(message = "Street is required")
        String street,
        
        @NotBlank(message = "City is required")
        String city,
        
        @NotBlank(message = "State is required")
        String state,
        
        @NotBlank(message = "Zip code is required")
        String zipCode,
        
        @NotBlank(message = "Country is required")
        String country
) {}
