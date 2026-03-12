package com.alquiler.furent.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void of_validAmount_createsMoney() {
        Money money = Money.of(25000, "COP");
        assertEquals(new BigDecimal("25000.00"), money.getAmount());
        assertEquals("COP", money.getCurrency());
    }

    @Test
    void of_bigDecimal_createsMoney() {
        Money money = Money.of(new BigDecimal("100.555"), "COP");
        assertEquals(new BigDecimal("100.56"), money.getAmount()); // rounded
    }

    @Test
    void of_negativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1, "COP"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.valueOf(-1), "COP"));
    }

    @Test
    void cop_factoryMethod_setsCOPCurrency() {
        Money money = Money.cop(50000);
        assertEquals("COP", money.getCurrency());
        assertEquals(50000.00, money.toDouble());
    }

    @Test
    void zero_createsZeroAmount() {
        Money money = Money.zero("COP");
        assertTrue(money.isZero());
        assertEquals(new BigDecimal("0.00"), money.getAmount());
    }

    @Test
    void add_sameCurrency_returnsSum() {
        Money a = Money.cop(100);
        Money b = Money.cop(200);
        Money result = a.add(b);
        assertEquals(new BigDecimal("300.00"), result.getAmount());
    }

    @Test
    void add_differentCurrency_throwsException() {
        Money cop = Money.cop(100);
        Money usd = Money.of(100, "USD");
        assertThrows(IllegalArgumentException.class, () -> cop.add(usd));
    }

    @Test
    void subtract_validAmount_returnsDifference() {
        Money a = Money.cop(500);
        Money b = Money.cop(200);
        Money result = a.subtract(b);
        assertEquals(new BigDecimal("300.00"), result.getAmount());
    }

    @Test
    void subtract_resultNegative_throwsException() {
        Money a = Money.cop(100);
        Money b = Money.cop(200);
        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
    }

    @Test
    void multiply_byInteger_returnsProduct() {
        Money price = Money.cop(15000);
        Money total = price.multiply(3);
        assertEquals(new BigDecimal("45000.00"), total.getAmount());
    }

    @Test
    void multiply_byNegativeInteger_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> Money.cop(100).multiply(-1));
    }

    @Test
    void multiply_byDouble_returnsProduct() {
        Money price = Money.cop(10000);
        Money result = price.multiply(1.5);
        assertEquals(new BigDecimal("15000.00"), result.getAmount());
    }

    @Test
    void applyDiscount_validPercentage_returnsDiscounted() {
        Money price = Money.cop(10000);
        Money discounted = price.applyDiscount(20);
        assertEquals(new BigDecimal("8000.00"), discounted.getAmount());
    }

    @Test
    void applyDiscount_invalidPercentage_throwsException() {
        Money price = Money.cop(10000);
        assertThrows(IllegalArgumentException.class, () -> price.applyDiscount(-5));
        assertThrows(IllegalArgumentException.class, () -> price.applyDiscount(101));
    }

    @Test
    void isGreaterThan_comparesCorrectly() {
        Money a = Money.cop(500);
        Money b = Money.cop(200);
        assertTrue(a.isGreaterThan(b));
        assertFalse(b.isGreaterThan(a));
    }

    @Test
    void format_returnsFormattedString() {
        Money money = Money.cop(25000);
        String formatted = money.format();
        assertTrue(formatted.contains("COP"));
        assertTrue(formatted.contains("25"));
    }

    @Test
    void equals_sameAmountAndCurrency_areEqual() {
        Money a = Money.cop(100);
        Money b = Money.cop(100);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentAmount_areNotEqual() {
        Money a = Money.cop(100);
        Money b = Money.cop(200);
        assertNotEquals(a, b);
    }

    @Test
    void equals_differentCurrency_areNotEqual() {
        Money a = Money.of(100, "COP");
        Money b = Money.of(100, "USD");
        assertNotEquals(a, b);
    }

    @Test
    void toString_returnsFormattedValue() {
        Money money = Money.cop(5000);
        assertNotNull(money.toString());
        assertTrue(money.toString().contains("COP"));
    }
}
