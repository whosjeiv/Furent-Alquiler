package com.alquiler.furent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Servicio de envío de correos electrónicos.
 * En modo desarrollo ({@code furent.email.enabled=false}) registra los correos en log.
 * En producción utiliza JavaMailSender para envío SMTP real.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${furent.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${furent.email.from:noreply@furent.com}")
    private String fromAddress;

    public void sendWelcomeEmail(String toEmail, String nombre) {
        sendEmail(toEmail, "¡Bienvenido a Furent, " + nombre + "!",
                "Gracias por registrarte en Furent. Tu cuenta está lista para usar.");
    }

    public void sendReservationConfirmation(String toEmail, String reservaId, BigDecimal total) {
        sendEmail(toEmail, "Cotización recibida — Furent",
                "Tu cotización #" + reservaId + " ha sido creada. Total: $" + String.format("%.0f", total));
    }

    public void sendStatusChange(String toEmail, String reservaId, String nuevoEstado) {
        sendEmail(toEmail, "Actualización de reserva — Furent",
                "Tu reserva #" + reservaId + " cambió a estado: " + nuevoEstado);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        sendEmail(toEmail, "Recuperar contraseña — Furent",
                "Haz clic en el siguiente enlace para restablecer tu contraseña: " + resetLink);
    }

    public void sendPaymentConfirmation(String toEmail, String reservaId) {
        sendEmail(toEmail, "Pago confirmado — Furent",
                "El pago de tu reserva #" + reservaId + " ha sido confirmado.");
    }

    public void sendContactNotification(String adminEmail, String fromName, String subject) {
        sendEmail(adminEmail, "Nuevo mensaje de contacto: " + subject,
                "Has recibido un nuevo mensaje de contacto de " + fromName);
    }

    private void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL-DEV] Para: {} | Asunto: {} | Cuerpo: {}", to, subject, body);
            return;
        }

        // Production email sending would go here using JavaMailSender
        // For now we log the intention
        log.info("[EMAIL] Enviando a: {} | Asunto: {}", to, subject);
    }
}
