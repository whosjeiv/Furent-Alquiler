package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPagosControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Principal principal;

    @InjectMocks
    private AdminPagosController controller;

    @Test
    void testListPayments() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay1");
        payment.setMonto(BigDecimal.valueOf(100));
        payment.setEstado("PENDIENTE");
        
        Page<Payment> page = new PageImpl<>(List.of(payment), PageRequest.of(0, 20), 1);
        when(paymentService.getAllPaymentsPaged(any(Pageable.class))).thenReturn(page);

        // Act
        String viewName = controller.listPayments(PageRequest.of(0, 20), model);

        // Assert
        assertEquals("admin/pagos", viewName);
        verify(model).addAttribute(eq("pagos"), any(Page.class));
        verify(paymentService).getAllPaymentsPaged(any(Pageable.class));
    }

    @Test
    void testConfirmPayment_Success() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay1");
        payment.setEstado("PAGADO");
        when(principal.getName()).thenReturn("admin@test.com");
        when(paymentService.confirmPayment(eq("pay1"), eq("REF123"), anyString())).thenReturn(payment);

        // Act
        String redirect = controller.confirmPayment("pay1", "REF123", principal, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/pagos", redirect);
        verify(redirectAttributes).addFlashAttribute("success", "Pago confirmado exitosamente");
        verify(paymentService).confirmPayment("pay1", "REF123", "admin@test.com");
    }

    @Test
    void testConfirmPayment_Error() {
        // Arrange
        when(principal.getName()).thenReturn("admin@test.com");
        when(paymentService.confirmPayment(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error de prueba"));

        // Act
        String redirect = controller.confirmPayment("pay1", "REF123", principal, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/pagos", redirect);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Error al confirmar pago"));
    }

    @Test
    void testRejectPayment_Success() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay1");
        payment.setEstado("FALLIDO");
        when(principal.getName()).thenReturn("admin@test.com");
        when(paymentService.failPayment(eq("pay1"), eq("Comprobante inválido"), anyString())).thenReturn(payment);

        // Act
        String redirect = controller.rejectPayment("pay1", "Comprobante inválido", principal, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/pagos", redirect);
        verify(redirectAttributes).addFlashAttribute("success", "Pago rechazado exitosamente");
        verify(paymentService).failPayment("pay1", "Comprobante inválido", "admin@test.com");
    }

    @Test
    void testRejectPayment_Error() {
        // Arrange
        when(principal.getName()).thenReturn("admin@test.com");
        when(paymentService.failPayment(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error de prueba"));

        // Act
        String redirect = controller.rejectPayment("pay1", "Motivo", principal, redirectAttributes);

        // Assert
        assertEquals("redirect:/admin/pagos", redirect);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Error al rechazar pago"));
    }

    @Test
    void testControllerAnnotations() {
        // Verify controller has correct annotations
        assertTrue(AdminPagosController.class.isAnnotationPresent(org.springframework.stereotype.Controller.class));
        assertTrue(AdminPagosController.class.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        assertTrue(AdminPagosController.class.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class));
        
        // Verify RequestMapping value
        org.springframework.web.bind.annotation.RequestMapping mapping = 
            AdminPagosController.class.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertArrayEquals(new String[]{"/admin/pagos"}, mapping.value());
        
        // Verify PreAuthorize value
        org.springframework.security.access.prepost.PreAuthorize preAuth = 
            AdminPagosController.class.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", preAuth.value());
    }
}
