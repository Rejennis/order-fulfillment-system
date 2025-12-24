package com.midlevel.orderfulfillment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money is a Value Object representing a monetary amount with currency.
 * 
 * DDD Value Object Properties:
 * - Immutable: All fields are final, no setters
 * - Self-validating: Constructor validates invariants
 * - Equality by value: equals() compares values, not identity
 */
public final class Money {
    
    // BigDecimal is used for precise decimal arithmetic (avoiding floating-point errors)
    private final BigDecimal amount;
    
    // Currency ensures we handle different currencies correctly
    private final Currency currency;
    
    /**
     * Private constructor to enforce factory method usage.
     * This allows us to control object creation and validation in one place.
     * 
     * @param amount the monetary amount
     * @param currency the currency code
     */
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    /**
     * Factory method to create a Money instance.
     * Using factory methods instead of public constructors gives us:
     * - Better naming (of() is clearer than new Money())
     * - Single point of validation
     * - Future flexibility (e.g., caching, subclassing)
     * 
     * @param amount the monetary amount
     * @param currencyCode the ISO 4217 currency code (e.g., "USD", "EUR")
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is null or negative, or currency is invalid
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        // Validate amount is not null
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        // Validate amount is not negative (business rule: no negative money)
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        
        // Validate currency code (Currency.getInstance throws if invalid)
        Currency currency = Currency.getInstance(currencyCode);
        
        // Scale the amount to the currency's standard decimal places (e.g., 2 for USD)
        BigDecimal scaledAmount = amount.setScale(
            currency.getDefaultFractionDigits(),  // Number of decimal places for this currency
            RoundingMode.HALF_UP                   // Round .5 up (standard rounding)
        );
        
        // Return the new Money instance
        return new Money(scaledAmount, currency);
    }
    
    /**
     * Convenience factory method for USD (most common use case).
     * 
     * @param amount the amount in USD
     * @return a new Money instance in USD
     */
    public static Money usd(BigDecimal amount) {
        return of(amount, "USD");
    }
    
    /**
     * Convenience factory method for creating Money from a double.
     * Warning: Use with caution due to floating-point precision issues.
     * 
     * @param amount the amount as a double
     * @param currencyCode the currency code
     * @return a new Money instance
     */
    public static Money of(double amount, String currencyCode) {
        // Convert double to BigDecimal (note: this can have precision issues)
        return of(BigDecimal.valueOf(amount), currencyCode);
    }
    
    /**
     * Adds another Money instance to this one.
     * Value objects are immutable, so this returns a new instance.
     * 
     * @param other the Money to add
     * @return a new Money instance with the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        // Validate we're not mixing currencies (business rule)
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot add different currencies: " + this.currency + " and " + other.currency
            );
        }
        
        // Add amounts and return new Money instance (immutability)
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Multiplies this Money by a quantity.
     * Used for calculating line item totals (price * quantity).
     * 
     * @param multiplier the number to multiply by
     * @return a new Money instance with the product
     */
    public Money multiply(int multiplier) {
        // Convert int to BigDecimal and multiply
        BigDecimal result = this.amount.multiply(BigDecimal.valueOf(multiplier));
        
        // Return new Money instance with proper scaling
        return new Money(
            result.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP),
            this.currency
        );
    }
    
    /**
     * Checks if this Money is greater than another Money.
     * 
     * @param other the Money to compare with
     * @return true if this amount is greater than other
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        // Validate same currency for comparison
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        
        // Compare amounts (returns > 0 if this > other)
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Checks if this Money represents zero amount.
     * 
     * @return true if amount is zero
     */
    public boolean isZero() {
        // Compare with ZERO (returns 0 if equal)
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    // Getters (no setters - immutability)
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    /**
     * Value objects are equal if their values are equal (not by identity).
     * This is crucial for DDD value objects.
     */
    @Override
    public boolean equals(Object o) {
        // Check if same reference (optimization)
        if (this == o) return true;
        
        // Check if null or different class
        if (o == null || getClass() != o.getClass()) return false;
        
        // Cast and compare field values
        Money money = (Money) o;
        
        // Compare amount (compareTo returns 0 if equal)
        // We use compareTo instead of equals to handle scale differences (2.00 vs 2.0)
        return amount.compareTo(money.amount) == 0 && 
               Objects.equals(currency, money.currency);
    }
    
    /**
     * Hash code must be consistent with equals.
     * Objects that are equal must have the same hash code.
     */
    @Override
    public int hashCode() {
        // Use Objects.hash for consistent hashing
        // Note: We use stripTrailingZeros() for amount to handle scale differences
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
    
    /**
     * Human-readable string representation.
     * Format: "100.00 USD"
     */
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
