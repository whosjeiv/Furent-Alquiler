package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AdminCuponesController.
 * Validates coupon management functionality including:
 * - Listing coupons
 * - Creating/editing coupons
 * - Validating discount percentage doesn't exceed 100%
 * - Deleting coupons
 */
@ExtendWith(MockitoExtension.class)
class AdminCuponesControllerTest {

    @Mock
    private CouponService couponService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminCuponesController controller;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testCoupon = new Coupon();
        testCoupon.setId("test-id");
        testCoupon.setCodigo("TEST20");
        testCoupon.setTipo("PORCENTAJE");
        testCoupon.setValor(BigDecimal.valueOf(20));
        testCoupon.setUsosMaximos(100);
        testCoupon.setUsosActuales(5);
    }

    @Test
    void listCoupons_shouldReturnCouponsView() {
        // Arrange
        List<Coupon> coupons = Arrays.asList(testCoupon);
        when(couponService.getAll()).thenReturn(coupons);

        // Act
        String viewName = controller.listCoupons(model);

        // Assert
        assertEquals("admin/cupones", viewName);
        verify(model).addAttribute("cupones", coupons);
        verify(couponService).getAll();
    }

    @Test
    void saveCoupon_withValidPercentageDiscount_shouldSaveSuccessfully() {
        // Arrange
        when(couponService.save(any(Coupon.class))).thenReturn(testCoupon);

        // Act
        String result = controller.saveCoupon(
            "TEST20",
            "PORCENTAJE",
            BigDecimal.valueOf(20),
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void saveCoupon_withDiscountOver100Percent_shouldReturnError() {
        // Act
        String result = controller.saveCoupon(
            "INVALID",
            "PORCENTAJE",
            BigDecimal.valueOf(150), // Invalid: > 100%
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService, never()).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("100%"));
    }

    @Test
    void saveCoupon_withExactly100Percent_shouldSaveSuccessfully() {
        // Arrange
        when(couponService.save(any(Coupon.class))).thenReturn(testCoupon);

        // Act
        String result = controller.saveCoupon(
            "FREE100",
            "PORCENTAJE",
            BigDecimal.valueOf(100), // Valid: exactly 100%
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void saveCoupon_withNegativeValue_shouldReturnError() {
        // Act
        String result = controller.saveCoupon(
            "INVALID",
            "PORCENTAJE",
            BigDecimal.valueOf(-10), // Invalid: negative
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService, never()).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("mayor a 0"));
    }

    @Test
    void saveCoupon_withZeroUsosMaximos_shouldReturnError() {
        // Act
        String result = controller.saveCoupon(
            "INVALID",
            "PORCENTAJE",
            BigDecimal.valueOf(20),
            "2024-01-01",
            "2024-12-31",
            0, // Invalid: zero
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService, never()).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("mayor a 0"));
    }

    @Test
    void saveCoupon_withFixedAmount_shouldNotValidatePercentage() {
        // Arrange
        when(couponService.save(any(Coupon.class))).thenReturn(testCoupon);

        // Act
        String result = controller.saveCoupon(
            "FIXED500",
            "MONTO_FIJO",
            BigDecimal.valueOf(500), // Fixed amount, no percentage validation
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            null,
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void saveCoupon_editingExistingCoupon_shouldUpdateCoupon() {
        // Arrange
        when(couponService.getById("test-id")).thenReturn(Optional.of(testCoupon));
        when(couponService.save(any(Coupon.class))).thenReturn(testCoupon);

        // Act
        String result = controller.saveCoupon(
            "TEST20",
            "PORCENTAJE",
            BigDecimal.valueOf(25),
            "2024-01-01",
            "2024-12-31",
            100,
            BigDecimal.ZERO,
            "test-id",
            redirectAttributes
        );

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).getById("test-id");
        verify(couponService).save(any(Coupon.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void deleteCoupon_shouldDeleteSuccessfully() {
        // Arrange
        doNothing().when(couponService).delete("test-id");

        // Act
        String result = controller.deleteCoupon("test-id", redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).delete("test-id");
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void deleteCoupon_withError_shouldReturnErrorMessage() {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(couponService).delete("test-id");

        // Act
        String result = controller.deleteCoupon("test-id", redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/cupones", result);
        verify(couponService).delete("test-id");
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("error"));
    }
}
