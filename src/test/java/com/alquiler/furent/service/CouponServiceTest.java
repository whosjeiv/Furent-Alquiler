package com.alquiler.furent.service;

import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    void validateCoupon_validPercentage_returnsDiscount() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("DESC20");
        coupon.setTipo("PORCENTAJE");
        coupon.setValor(BigDecimal.valueOf(20));
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(0);
        coupon.setActivo(true);
        coupon.setMontoMinimo(BigDecimal.ZERO);

        when(couponRepository.findByCodigoIgnoreCase("DESC20")).thenReturn(Optional.of(coupon));

        Map<String, Object> result = couponService.validateCoupon("DESC20", BigDecimal.valueOf(100000));

        assertTrue((Boolean) result.get("valido"));
        assertEquals(0, new BigDecimal("20000.00").compareTo((BigDecimal) result.get("descuento")));
        assertEquals(0, new BigDecimal("80000.00").compareTo((BigDecimal) result.get("montoFinal")));
    }

    @Test
    void validateCoupon_fixedAmount_returnsDiscount() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("FIJO10K");
        coupon.setTipo("MONTO_FIJO");
        coupon.setValor(BigDecimal.valueOf(10000));
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(50);
        coupon.setUsosActuales(0);
        coupon.setActivo(true);
        coupon.setMontoMinimo(BigDecimal.ZERO);

        when(couponRepository.findByCodigoIgnoreCase("FIJO10K")).thenReturn(Optional.of(coupon));

        Map<String, Object> result = couponService.validateCoupon("FIJO10K", BigDecimal.valueOf(80000));

        assertTrue((Boolean) result.get("valido"));
        assertEquals(0, new BigDecimal("10000").compareTo((BigDecimal) result.get("descuento")));
        assertEquals(0, new BigDecimal("70000").compareTo((BigDecimal) result.get("montoFinal")));
    }

    @Test
    void validateCoupon_notFound_returnsInvalid() {
        when(couponRepository.findByCodigoIgnoreCase("NOEXISTE")).thenReturn(Optional.empty());

        Map<String, Object> result = couponService.validateCoupon("NOEXISTE", BigDecimal.valueOf(50000));

        assertFalse((Boolean) result.get("valido"));
    }

    @Test
    void validateCoupon_expired_returnsInvalid() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("EXPIRED");
        coupon.setTipo("PORCENTAJE");
        coupon.setValor(BigDecimal.valueOf(10));
        coupon.setValidoDesde(LocalDate.now().minusDays(30));
        coupon.setValidoHasta(LocalDate.now().minusDays(1));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(0);
        coupon.setActivo(true);
        coupon.setMontoMinimo(BigDecimal.ZERO);

        when(couponRepository.findByCodigoIgnoreCase("EXPIRED")).thenReturn(Optional.of(coupon));

        Map<String, Object> result = couponService.validateCoupon("EXPIRED", BigDecimal.valueOf(50000));

        assertFalse((Boolean) result.get("valido"));
    }

    @Test
    void validateCoupon_belowMinimum_returnsInvalid() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("MIN50K");
        coupon.setTipo("PORCENTAJE");
        coupon.setValor(BigDecimal.valueOf(15));
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(0);
        coupon.setActivo(true);
        coupon.setMontoMinimo(BigDecimal.valueOf(50000));

        when(couponRepository.findByCodigoIgnoreCase("MIN50K")).thenReturn(Optional.of(coupon));

        Map<String, Object> result = couponService.validateCoupon("MIN50K", BigDecimal.valueOf(20000));

        assertFalse((Boolean) result.get("valido"));
    }

    // Tests for new validateCoupon(String codigo) method
    @Test
    void validateCouponByCodigo_validCoupon_returnsCoupon() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("VALID20");
        coupon.setTipo("PORCENTAJE");
        coupon.setValor(BigDecimal.valueOf(20));
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(50);
        coupon.setActivo(true);

        when(couponRepository.findByCodigoIgnoreCase("VALID20")).thenReturn(Optional.of(coupon));

        Coupon result = couponService.validateCoupon("VALID20");

        assertNotNull(result);
        assertEquals("VALID20", result.getCodigo());
        verify(couponRepository).findByCodigoIgnoreCase("VALID20");
    }

    @Test
    void validateCouponByCodigo_notFound_throwsException() {
        when(couponRepository.findByCodigoIgnoreCase("NOTFOUND")).thenReturn(Optional.empty());

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.validateCoupon("NOTFOUND"));

        assertEquals("Cupón no encontrado", exception.getMessage());
    }

    @Test
    void validateCouponByCodigo_inactive_throwsException() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("INACTIVE");
        coupon.setActivo(false);
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(0);

        when(couponRepository.findByCodigoIgnoreCase("INACTIVE")).thenReturn(Optional.of(coupon));

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.validateCoupon("INACTIVE"));

        assertEquals("El cupón no está activo", exception.getMessage());
    }

    @Test
    void validateCouponByCodigo_expired_throwsException() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("EXPIRED");
        coupon.setActivo(true);
        coupon.setValidoDesde(LocalDate.now().minusDays(30));
        coupon.setValidoHasta(LocalDate.now().minusDays(1));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(0);

        when(couponRepository.findByCodigoIgnoreCase("EXPIRED")).thenReturn(Optional.of(coupon));

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.validateCoupon("EXPIRED"));

        assertEquals("El cupón ha expirado o aún no es válido", exception.getMessage());
    }

    @Test
    void validateCouponByCodigo_reachedLimit_throwsException() {
        Coupon coupon = new Coupon();
        coupon.setCodigo("MAXED");
        coupon.setActivo(true);
        coupon.setValidoDesde(LocalDate.now().minusDays(1));
        coupon.setValidoHasta(LocalDate.now().plusDays(10));
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(100);

        when(couponRepository.findByCodigoIgnoreCase("MAXED")).thenReturn(Optional.of(coupon));

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.validateCoupon("MAXED"));

        assertEquals("El cupón ha alcanzado su límite de usos", exception.getMessage());
    }

    // Tests for applyDiscount method
    @Test
    void applyDiscount_percentageCoupon_calculatesCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setTipo("PORCENTAJE");
        coupon.setValor(BigDecimal.valueOf(15));

        BigDecimal total = BigDecimal.valueOf(1000);
        BigDecimal result = couponService.applyDiscount(coupon, total);

        // 1000 * (1 - 0.15) = 850
        assertEquals(0, new BigDecimal("850.00").compareTo(result));
    }

    @Test
    void applyDiscount_fixedAmountCoupon_calculatesCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setTipo("MONTO_FIJO");
        coupon.setValor(BigDecimal.valueOf(100));

        BigDecimal total = BigDecimal.valueOf(500);
        BigDecimal result = couponService.applyDiscount(coupon, total);

        // 500 - 100 = 400
        assertEquals(0, new BigDecimal("400.00").compareTo(result));
    }

    @Test
    void applyDiscount_fixedAmountExceedsTotal_returnsZero() {
        Coupon coupon = new Coupon();
        coupon.setTipo("MONTO_FIJO");
        coupon.setValor(BigDecimal.valueOf(1000));

        BigDecimal total = BigDecimal.valueOf(500);
        BigDecimal result = couponService.applyDiscount(coupon, total);

        // 500 - 1000 = -500, but should return 0
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void applyDiscount_unknownType_returnsOriginalTotal() {
        Coupon coupon = new Coupon();
        coupon.setTipo("UNKNOWN");
        coupon.setValor(BigDecimal.valueOf(50));

        BigDecimal total = BigDecimal.valueOf(1000);
        BigDecimal result = couponService.applyDiscount(coupon, total);

        assertEquals(total, result);
    }

    // Tests for incrementUsage method
    @Test
    void incrementUsage_validCoupon_incrementsCounter() {
        Coupon coupon = new Coupon();
        coupon.setId("coupon123");
        coupon.setCodigo("TEST");
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(50);

        when(couponRepository.findById("coupon123")).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        couponService.incrementUsage("coupon123");

        assertEquals(51, coupon.getUsosActuales());
        verify(couponRepository).save(coupon);
    }

    @Test
    void incrementUsage_couponNotFound_throwsException() {
        when(couponRepository.findById("notfound")).thenReturn(Optional.empty());

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.incrementUsage("notfound"));

        assertEquals("Cupón no encontrado", exception.getMessage());
    }

    @Test
    void incrementUsage_reachedLimit_throwsException() {
        Coupon coupon = new Coupon();
        coupon.setId("coupon123");
        coupon.setCodigo("MAXED");
        coupon.setUsosMaximos(100);
        coupon.setUsosActuales(100);

        when(couponRepository.findById("coupon123")).thenReturn(Optional.of(coupon));

        InvalidOperationException exception = assertThrows(InvalidOperationException.class, 
            () -> couponService.incrementUsage("coupon123"));

        assertEquals("El cupón ha alcanzado su límite de usos", exception.getMessage());
        verify(couponRepository, never()).save(any());
    }
}
