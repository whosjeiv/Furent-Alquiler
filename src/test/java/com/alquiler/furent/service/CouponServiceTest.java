package com.alquiler.furent.service;

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
}
