package com.alquiler.furent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendWelcomeEmail_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail("user@test.com", "Juan"));
    }

    @Test
    void sendReservationConfirmation_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendReservationConfirmation(
                "user@test.com", "res-001", java.math.BigDecimal.valueOf(150000)));
    }

    @Test
    void sendStatusChange_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendStatusChange(
                "user@test.com", "res-001", "CONFIRMADA"));
    }

    @Test
    void sendPasswordResetEmail_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(
                "user@test.com", "https://furent.com/reset/abc123"));
    }

    @Test
    void sendPaymentConfirmation_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendPaymentConfirmation(
                "user@test.com", "res-001"));
    }

    @Test
    void sendContactNotification_devMode_doesNotThrow() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        assertDoesNotThrow(() -> emailService.sendContactNotification(
                "admin@furent.com", "Juan Pérez", "juan@test.com", "Consulta sobre alquiler", "Mensaje de prueba", "123456789"));
    }
}
