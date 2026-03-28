package com.alquiler.furent.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    @Test
    void isValid_activeCoupon_withinDates_returnsTrue() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setValidoDesde(LocalDate.now().minusDays(1));
        c.setValidoHasta(LocalDate.now().plusDays(5));
        c.setUsosMaximos(10);
        c.setUsosActuales(3);

        assertTrue(c.isValid());
    }

    @Test
    void isValid_expiredCoupon_returnsFalse() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setValidoHasta(LocalDate.now().minusDays(1));

        assertFalse(c.isValid());
    }

    @Test
    void isValid_inactiveCoupon_returnsFalse() {
        Coupon c = new Coupon();
        c.setActivo(false);

        assertFalse(c.isValid());
    }

    @Test
    void isValid_usosAgotados_returnsFalse() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setUsosMaximos(5);
        c.setUsosActuales(5);

        assertFalse(c.isValid());
    }

    @Test
    void isValid_noMaxUsos_alwaysValid() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setUsosMaximos(0); // 0 means unlimited
        c.setUsosActuales(999);

        assertTrue(c.isValid());
    }

    @Test
    void isValid_beforeValidoDesde_returnsFalse() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setValidoDesde(LocalDate.now().plusDays(5));

        assertFalse(c.isValid());
    }

    @Test
    void calcularDescuento_porcentaje_calculatesCorrectly() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setTipo("PORCENTAJE");
        c.setValor(BigDecimal.valueOf(20)); // 20%
        c.setMontoMinimo(BigDecimal.ZERO);

        BigDecimal descuento = c.calcularDescuento(BigDecimal.valueOf(100000));

        assertEquals(0, descuento.compareTo(BigDecimal.valueOf(20000)));
    }

    @Test
    void calcularDescuento_montoFijo_appliesFixedAmount() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setTipo("MONTO_FIJO");
        c.setValor(BigDecimal.valueOf(15000));
        c.setMontoMinimo(BigDecimal.ZERO);

        BigDecimal descuento = c.calcularDescuento(BigDecimal.valueOf(100000));

        assertEquals(0, descuento.compareTo(BigDecimal.valueOf(15000)));
    }

    @Test
    void calcularDescuento_montoFijo_cannotExceedTotal() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setTipo("MONTO_FIJO");
        c.setValor(BigDecimal.valueOf(50000));
        c.setMontoMinimo(BigDecimal.ZERO);

        BigDecimal descuento = c.calcularDescuento(BigDecimal.valueOf(30000));

        assertEquals(0, descuento.compareTo(BigDecimal.valueOf(30000)));
    }

    @Test
    void calcularDescuento_belowMinimo_returnsZero() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setTipo("PORCENTAJE");
        c.setValor(BigDecimal.valueOf(10));
        c.setMontoMinimo(BigDecimal.valueOf(100000));

        BigDecimal descuento = c.calcularDescuento(BigDecimal.valueOf(50000));

        assertEquals(0, descuento.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularDescuento_invalidCoupon_returnsZero() {
        Coupon c = new Coupon();
        c.setActivo(false);
        c.setTipo("PORCENTAJE");
        c.setValor(BigDecimal.valueOf(10));

        BigDecimal descuento = c.calcularDescuento(BigDecimal.valueOf(100000));

        assertEquals(0, descuento.compareTo(BigDecimal.ZERO));
    }

    @Test
    void constructor_setsDefaults() {
        Coupon c = new Coupon();

        assertTrue(c.isActivo());
        assertEquals(0, c.getUsosActuales());
    }

    @Test
    void isVigente_withinDateRange_returnsTrue() {
        Coupon c = new Coupon();
        c.setValidoDesde(LocalDate.now().minusDays(5));
        c.setValidoHasta(LocalDate.now().plusDays(5));

        assertTrue(c.isVigente());
    }

    @Test
    void isVigente_beforeValidoDesde_returnsFalse() {
        Coupon c = new Coupon();
        c.setValidoDesde(LocalDate.now().plusDays(1));
        c.setValidoHasta(LocalDate.now().plusDays(10));

        assertFalse(c.isVigente());
    }

    @Test
    void isVigente_afterValidoHasta_returnsFalse() {
        Coupon c = new Coupon();
        c.setValidoDesde(LocalDate.now().minusDays(10));
        c.setValidoHasta(LocalDate.now().minusDays(1));

        assertFalse(c.isVigente());
    }

    @Test
    void isVigente_nullDates_returnsTrue() {
        Coupon c = new Coupon();
        c.setValidoDesde(null);
        c.setValidoHasta(null);

        assertTrue(c.isVigente());
    }

    @Test
    void isVigente_onlyValidoDesde_checksStartDate() {
        Coupon c = new Coupon();
        c.setValidoDesde(LocalDate.now().minusDays(1));
        c.setValidoHasta(null);

        assertTrue(c.isVigente());
    }

    @Test
    void isVigente_onlyValidoHasta_checksEndDate() {
        Coupon c = new Coupon();
        c.setValidoDesde(null);
        c.setValidoHasta(LocalDate.now().plusDays(1));

        assertTrue(c.isVigente());
    }

    @Test
    void hasReachedLimit_belowLimit_returnsFalse() {
        Coupon c = new Coupon();
        c.setUsosMaximos(10);
        c.setUsosActuales(5);

        assertFalse(c.hasReachedLimit());
    }

    @Test
    void hasReachedLimit_atLimit_returnsTrue() {
        Coupon c = new Coupon();
        c.setUsosMaximos(10);
        c.setUsosActuales(10);

        assertTrue(c.hasReachedLimit());
    }

    @Test
    void hasReachedLimit_aboveLimit_returnsTrue() {
        Coupon c = new Coupon();
        c.setUsosMaximos(10);
        c.setUsosActuales(15);

        assertTrue(c.hasReachedLimit());
    }

    @Test
    void hasReachedLimit_unlimitedUses_returnsFalse() {
        Coupon c = new Coupon();
        c.setUsosMaximos(0); // 0 means unlimited
        c.setUsosActuales(999);

        assertFalse(c.hasReachedLimit());
    }

    @Test
    void hasReachedLimit_negativeMaxUsos_returnsFalse() {
        Coupon c = new Coupon();
        c.setUsosMaximos(-1);
        c.setUsosActuales(10);

        assertFalse(c.hasReachedLimit());
    }

    @Test
    void isValid_combinesAllValidations() {
        Coupon c = new Coupon();
        c.setActivo(true);
        c.setValidoDesde(LocalDate.now().minusDays(1));
        c.setValidoHasta(LocalDate.now().plusDays(5));
        c.setUsosMaximos(10);
        c.setUsosActuales(3);

        assertTrue(c.isValid());
        assertTrue(c.isVigente());
        assertFalse(c.hasReachedLimit());
    }
}
