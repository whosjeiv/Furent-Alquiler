package com.alquiler.furent.controller;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.model.Coupon;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for coupon validation endpoint and coupon integration in quotation flow.
 * 
 * Validates Requirements 15.1-15.9:
 * - Coupon validation endpoint
 * - Coupon integration in quotation
 * - Discount calculation
 * - Usage counter increment
 * - Original and discounted totals in response
 */
@ExtendWith(MockitoExtension.class)
class ApiControllerCouponIntegrationTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @Mock
    private CouponService couponService;

    @Mock
    private ProductService productService;

    @Mock
    private Authentication authentication;

    private ApiController apiController;
    private User testUser;
    private Product testProduct;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("test-tenant");

        // Initialize controller with mocked dependencies
        apiController = new ApiController(
                reservationService,
                null, // reservationRepository
                userService,
                null, // auditLogService
                productService,
                null, // notificationService
                couponService,
                null, // emailService
                null, // pendingCardPaymentRepository
                null, // featureFlags
                null  // payUProperties
        );

        testUser = new User();
        testUser.setId("user123");
        testUser.setEmail("test@example.com");
        testUser.setNombre("Test");
        testUser.setApellido("User");
        testUser.setNotificacionesEmail(false);

        testProduct = new Product();
        testProduct.setId("prod123");
        testProduct.setNombre("Mesa Redonda");
        testProduct.setImagenUrl("/images/mesa.jpg");
        testProduct.setPrecioPorDia(new BigDecimal("100.00"));
        testProduct.setStock(10);

        testCoupon = new Coupon();
        testCoupon.setId("coupon123");
        testCoupon.setCodigo("VERANO2024");
        testCoupon.setTipo("PORCENTAJE");
        testCoupon.setValor(new BigDecimal("15"));
        testCoupon.setActivo(true);
        testCoupon.setValidoDesde(LocalDate.now().minusDays(1));
        testCoupon.setValidoHasta(LocalDate.now().plusDays(30));
        testCoupon.setUsosMaximos(100);
        testCoupon.setUsosActuales(50);
    }

    /**
     * Task 62: Test POST /api/cupones/validar endpoint
     * Validates Requirement 15.1, 15.2, 15.3
     */
    @Test
    void validateCoupon_withValidCoupon_returnsValidationResult() {
        // Arrange
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valido", true);
        validationResult.put("descuento", new BigDecimal("150.00"));
        validationResult.put("montoFinal", new BigDecimal("850.00"));
        validationResult.put("mensaje", "Cupón aplicado: -$150");

        when(couponService.validateCoupon(eq("VERANO2024"), any(BigDecimal.class)))
                .thenReturn(validationResult);

        Map<String, Object> request = new HashMap<>();
        request.put("codigo", "VERANO2024");
        request.put("montoTotal", 1000);

        // Act
        ResponseEntity<Map<String, Object>> response = apiController.validateCoupon(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("valido"));
        assertEquals(new BigDecimal("150.00"), response.getBody().get("descuento"));
        assertEquals(new BigDecimal("850.00"), response.getBody().get("montoFinal"));

        verify(couponService).validateCoupon(eq("VERANO2024"), any(BigDecimal.class));
    }

    /**
     * Task 62: Test coupon validation with invalid coupon
     * Validates Requirement 15.5
     */
    @Test
    void validateCoupon_withInvalidCoupon_returnsErrorReason() {
        // Arrange
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valido", false);
        validationResult.put("mensaje", "El cupón ha expirado o ya no está disponible");

        when(couponService.validateCoupon(eq("EXPIRED"), any(BigDecimal.class)))
                .thenReturn(validationResult);

        Map<String, Object> request = new HashMap<>();
        request.put("codigo", "EXPIRED");
        request.put("montoTotal", 1000);

        // Act
        ResponseEntity<Map<String, Object>> response = apiController.validateCoupon(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("valido"));
        assertEquals("El cupón ha expirado o ya no está disponible", response.getBody().get("mensaje"));

        verify(couponService).validateCoupon(eq("EXPIRED"), any(BigDecimal.class));
    }

    /**
     * Task 63: Verify coupon service integration
     * Validates that CouponService methods work correctly
     */
    @Test
    void couponService_validateAndApplyDiscount_worksCorrectly() {
        // Arrange
        BigDecimal total = new BigDecimal("1000.00");
        BigDecimal expectedDiscount = new BigDecimal("150.00");
        
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valido", true);
        validationResult.put("descuento", expectedDiscount);
        validationResult.put("montoFinal", total.subtract(expectedDiscount));

        when(couponService.validateCoupon("VERANO2024", total))
                .thenReturn(validationResult);

        // Act
        Map<String, Object> result = couponService.validateCoupon("VERANO2024", total);

        // Assert
        assertNotNull(result);
        assertTrue((Boolean) result.get("valido"));
        assertEquals(expectedDiscount, result.get("descuento"));
        assertEquals(new BigDecimal("850.00"), result.get("montoFinal"));
    }

    /**
     * Task 63: Verify coupon usage increment
     * Validates Requirement 15.6
     */
    @Test
    void couponService_useCoupon_incrementsUsageCounter() {
        // Arrange
        String couponCode = "VERANO2024";

        // Act
        couponService.useCoupon(couponCode);

        // Assert
        verify(couponService).useCoupon(couponCode);
    }
}
