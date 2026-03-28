package com.alquiler.furent.service;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserService userService;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendWelcomeEmail_doesNotThrow() {
        User user = new User();
        user.setNombre("Juan");
        user.setEmail("user@test.com");
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(user));
    }

    @Test
    void sendReservationConfirmation_doesNotThrow() {
        Reservation reservation = new Reservation();
        reservation.setId("res-001");
        reservation.setUsuarioEmail("user@test.com");
        reservation.setTotal(java.math.BigDecimal.valueOf(150000));
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendReservationConfirmation(reservation));
    }

    @Test
    void sendStatusChange_doesNotThrow() {
        Reservation reservation = new Reservation();
        reservation.setId("res-001");
        reservation.setUsuarioEmail("user@test.com");
        reservation.setEstado("CONFIRMADA");
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendStatusChange(reservation, "PENDIENTE"));
    }

    @Test
    void sendPasswordResetEmail_doesNotThrow() {
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendPasswordResetLink(
                "user@test.com", "https://furent.com/reset/abc123"));
    }

    @Test
    void sendPaymentConfirmation_doesNotThrow() {
        Payment payment = new Payment();
        payment.setReservaId("res-001");
        payment.setUsuarioId("user-001");
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendPaymentConfirmation(payment));
    }

    @Test
    void sendContactNotification_doesNotThrow() {
        // Email sending will fail but should not throw exception (logged internally)
        assertDoesNotThrow(() -> emailService.sendContactNotification(
                "admin@furent.com", "Juan Pérez", "juan@test.com", "Consulta sobre alquiler", "Mensaje de prueba", "123456789"));
    }
}
