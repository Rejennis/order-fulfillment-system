package com.midlevel.orderfulfillment.domain.model;

import java.util.Objects;

/**
 * Address is a Value Object representing a shipping or billing address.
 * 
 * DDD Value Object: Immutable, self-validating, and equality by value.
 */
public final class Address {
    
    private final String street;        // Street address (e.g., "123 Main St")
    private final String city;          // City name
    private final String state;         // State/Province code (e.g., "CA", "NY")
    private final String postalCode;    // Postal/ZIP code
    private final String country;       // Country code (e.g., "US", "CA")
    
    /**
     * Private constructor to enforce factory method pattern.
     */
    private Address(String street, String city, String state, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    /**
     * Factory method to create an Address with validation.
     * 
     * Business rules:
     * - All fields are required (no nulls or empty strings)
     * - Postal code format can vary by country (simplified here)
     * 
     * @param street the street address
     * @param city the city name
     * @param state the state/province code
     * @param postalCode the postal/ZIP code
     * @param country the country code
     * @return a new Address instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Address of(String street, String city, String state, String postalCode, String country) {
        // Validate street address
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street address cannot be null or empty");
        }
        
        // Validate city
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        
        // Validate state
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State cannot be null or empty");
        }
        
        // Validate postal code
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be null or empty");
        }
        
        // Validate country code
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
        
        // Additional validation: Country code should be 2 characters (ISO 3166-1 alpha-2)
        if (country.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters (ISO 3166-1 alpha-2): " + country);
        }
        
        // All validations passed - create the address
        return new Address(
            street.trim(),
            city.trim(),
            state.trim().toUpperCase(),      // Normalize state to uppercase
            postalCode.trim(),
            country.trim().toUpperCase()     // Normalize country to uppercase
        );
    }
    
    /**
     * Convenience factory method for US addresses.
     * Pre-fills country as "US".
     */
    public static Address usAddress(String street, String city, String state, String zipCode) {
        return of(street, city, state, zipCode, "US");
    }
    
    /**
     * Formats the address as a multi-line string.
     * Useful for display on shipping labels or invoices.
     * 
     * @return formatted address string
     */
    public String toFormattedString() {
        // Build multi-line string using StringBuilder for efficiency
        StringBuilder sb = new StringBuilder();
        sb.append(street).append("\n");
        sb.append(city).append(", ").append(state).append(" ").append(postalCode).append("\n");
        sb.append(country);
        return sb.toString();
    }
    
    /**
     * Checks if this is a US address.
     * Useful for applying US-specific shipping rules.
     * 
     * @return true if country is US
     */
    public boolean isUS() {
        return "US".equals(country);
    }
    
    // Getters (no setters - immutability)
    
    public String getStreet() {
        return street;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getState() {
        return state;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    /**
     * Value object equality: equal if all address components are equal.
     * Two addresses with identical street, city, state, postal code, and country are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Address address = (Address) o;
        return Objects.equals(street, address.street) &&
               Objects.equals(city, address.city) &&
               Objects.equals(state, address.state) &&
               Objects.equals(postalCode, address.postalCode) &&
               Objects.equals(country, address.country);
    }
    
    /**
     * Hash code consistent with equals.
     */
    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, postalCode, country);
    }
    
    /**
     * Single-line string representation.
     */
    @Override
    public String toString() {
        return String.format("%s, %s, %s %s, %s", street, city, state, postalCode, country);
    }
}
