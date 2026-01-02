package com.midlevel.orderfulfillment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderItem Value Object Tests")
class OrderItemTest {

    @Test
    @DisplayName("Creates with valid inputs and computes line total")
    void createsValidAndComputesLineTotal() {
        OrderItem item = OrderItem.of("PROD-1", "Widget", Money.usd(BigDecimal.valueOf(12.50)), 3);
        assertEquals("PROD-1", item.getProductId());
        assertEquals("Widget", item.getProductName());
        assertEquals(3, item.getQuantity());
        assertEquals(Money.usd(BigDecimal.valueOf(37.50)), item.calculateLineTotal());
    }

    @Test
    @DisplayName("Rejects null/blank productId and productName")
    void rejectsInvalidProductFields() {
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of(null, "Name", Money.usd(BigDecimal.ONE), 1));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("   ", "Name", Money.usd(BigDecimal.ONE), 1));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("P", null, Money.usd(BigDecimal.ONE), 1));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("P", "   ", Money.usd(BigDecimal.ONE), 1));
    }

    @Test
    @DisplayName("Rejects null unit price and non-positive quantity")
    void rejectsNullPriceAndNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("P", "N", null, 1));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("P", "N", Money.usd(BigDecimal.ONE), 0));
        assertThrows(IllegalArgumentException.class, () -> OrderItem.of("P", "N", Money.usd(BigDecimal.ONE), -5));
    }

    @Test
    @DisplayName("withQuantity returns new instance; immutability preserved")
    void withQuantityIsImmutable() {
        OrderItem original = OrderItem.of("PROD-9", "Thing", Money.usd(BigDecimal.valueOf(2.00)), 2);
        OrderItem updated = original.withQuantity(5);

        assertNotSame(original, updated);
        assertEquals(2, original.getQuantity());
        assertEquals(5, updated.getQuantity());
        assertEquals(Money.usd(BigDecimal.valueOf(10.00)), updated.calculateLineTotal());
    }

    @Test
    @DisplayName("Equality and hashCode are value-based")
    void equalityAndHash() {
        OrderItem a = OrderItem.of("P1", "Name", Money.usd(BigDecimal.valueOf(3.00)), 2);
        OrderItem b = OrderItem.of("P1", "Name", Money.usd(BigDecimal.valueOf(3.00)), 2);
        OrderItem c = OrderItem.of("P1", "Name", Money.usd(BigDecimal.valueOf(3.00)), 3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
