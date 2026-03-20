package com.alquiler.furent.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Servicio de envío de correos electrónicos.
 * Utiliza JavaMailSender para envío SMTP real a través de Gmail.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String toEmail, String nombre) {
        String subject = "¡Bienvenido a Furent, " + nombre + "! 🎉";
        String html = buildGenericHtmlEmail(
                "¡Bienvenido, " + nombre + "!",
                "Gracias por registrarte en <strong>Furent</strong>. Tu cuenta está lista para usar.<br><br>" +
                        "Ahora puedes explorar nuestro catálogo de mobiliarios para eventos y crear tus cotizaciones."
        );
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendReservationConfirmation(String toEmail, String reservaId, BigDecimal total) {
        String subject = "Cotización recibida — Furent";
        String html = buildGenericHtmlEmail(
                "Cotización Recibida",
                "Tu cotización <strong>#" + reservaId + "</strong> ha sido creada exitosamente.<br><br>" +
                        "Total: <strong>$" + String.format("%.0f", total) + "</strong><br><br>" +
                        "Pronto nos pondremos en contacto contigo para confirmar los detalles."
        );
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendStatusChange(String toEmail, String reservaId, String nuevoEstado) {
        String subject = "Actualización de reserva — Furent";
        String html = buildGenericHtmlEmail(
                "Actualización de Reserva",
                "Tu reserva <strong>#" + reservaId + "</strong> cambió a estado: <strong>" + nuevoEstado + "</strong>."
        );
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Recuperar contraseña — Furent";
        String html = buildPasswordResetHtmlEmail(resetLink);
        sendHtmlEmail(toEmail, subject, html);
    }

    private String buildPasswordResetHtmlEmail(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f1fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1fa;padding:32px 0;">
                    <tr><td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(124,58,237,0.08);">
                            <tr>
                                <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);padding:28px 32px;text-align:center;">
                                    <h1 style="margin:0;font-size:24px;color:#ffffff;letter-spacing:1px;">FURENT</h1>
                                    <p style="margin:4px 0 0;font-size:11px;color:rgba(255,255,255,0.75);letter-spacing:0.5px;">Alquiler de Mobiliarios para Eventos</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:32px;">
                                    <h2 style="margin:0 0 16px;font-size:20px;color:#1f2937;">Recuperar Contraseña 🔑</h2>
                                    <p style="margin:0 0 16px;font-size:14px;line-height:1.7;color:#4b5563;">
                                        Recibimos una solicitud para restablecer tu contraseña. Si no fuiste tú, puedes ignorar este correo.
                                    </p>
                                    <p style="margin:0 0 24px;font-size:14px;line-height:1.7;color:#4b5563;">
                                        Haz clic en el botón para crear una nueva contraseña. Este enlace expira en <strong>1 hora</strong>.
                                    </p>
                                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 24px;">
                                        <tr>
                                            <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);border-radius:12px;padding:16px 48px;">
                                                <a href="%s" style="color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;">Restablecer Contraseña</a>
                                            </td>
                                        </tr>
                                    </table>
                                    <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
                                        Si el botón no funciona, copia y pega este enlace:<br/>
                                        <span style="color:#7c3aed;word-break:break-all;">%s</span>
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:0 32px 24px;">
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#fef3c7;border-radius:10px;border:1px solid #fde68a;">
                                        <tr>
                                            <td style="padding:14px 18px;">
                                                <p style="margin:0;font-size:12px;color:#92400e;line-height:1.5;">
                                                    ⚠️ <strong>Seguridad:</strong> Nunca compartas este enlace. Furent nunca te pedirá tu contraseña.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:20px 32px;background-color:#faf5ff;border-top:1px solid #ede9fe;text-align:center;">
                                    <p style="margin:0;font-size:11px;color:#9ca3af;">Este correo fue enviado automáticamente desde Furent.</p>
                                    <p style="margin:4px 0 0;font-size:11px;color:#9ca3af;">© 2026 Furent — Todos los derechos reservados.</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
                </body>
                </html>
                """.formatted(resetLink, resetLink);
    }

    public void sendPasswordChangedEmail(String toEmail, String nombre, String resetUrl) {
        String subject = "Tu contraseña fue cambiada — Furent";
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f1fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1fa;padding:32px 0;">
                    <tr><td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(124,58,237,0.08);">
                            <tr>
                                <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);padding:28px 32px;text-align:center;">
                                    <h1 style="margin:0;font-size:24px;color:#ffffff;letter-spacing:1px;">FURENT</h1>
                                    <p style="margin:4px 0 0;font-size:11px;color:rgba(255,255,255,0.75);">Alquiler de Mobiliarios para Eventos</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:32px;">
                                    <h2 style="margin:0 0 12px;font-size:20px;color:#1f2937;">Contraseña cambiada exitosamente ✅</h2>
                                    <p style="margin:0 0 16px;font-size:14px;line-height:1.7;color:#4b5563;">
                                        Hola <strong>%s</strong>, te informamos que la contraseña de tu cuenta fue cambiada correctamente.
                                    </p>
                                    <p style="margin:0 0 24px;font-size:14px;line-height:1.7;color:#4b5563;">
                                        Si <strong>fuiste tú</strong> quien realizó este cambio, puedes ignorar este correo. Tu cuenta está segura.
                                    </p>
                                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 24px;">
                                        <tr>
                                            <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);border-radius:12px;padding:16px 32px;">
                                                <a href="%s" style="color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;">Restablecer contraseña ahora</a>
                                            </td>
                                        </tr>
                                    </table>
                                    <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;text-align:center;">
                                        ¿No fuiste tú? <a href="mailto:soporte@furent.com" style="color:#7c3aed;">Contáctanos de inmediato</a>
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:0 32px 24px;">
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#fef3c7;border-radius:10px;border:1px solid #fde68a;">
                                        <tr>
                                            <td style="padding:14px 18px;">
                                                <p style="margin:0;font-size:12px;color:#92400e;line-height:1.5;">
                                                    ⚠️ <strong>Importante:</strong> Si no reconoces esta actividad, restablece tu contraseña de inmediato y contacta a soporte.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:20px 32px;background-color:#faf5ff;border-top:1px solid #ede9fe;text-align:center;">
                                    <p style="margin:0;font-size:11px;color:#9ca3af;">© 2026 Furent — Todos los derechos reservados.</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
                </body>
                </html>
                """.formatted(nombre, resetUrl);
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendPaymentConfirmation(String toEmail, String reservaId) {
        String subject = "Pago confirmado — Furent";
        String html = buildGenericHtmlEmail(
                "Pago Confirmado",
                "El pago de tu reserva <strong>#" + reservaId + "</strong> ha sido confirmado.<br><br>¡Gracias por confiar en Furent!"
        );
        sendHtmlEmail(toEmail, subject, html);
    }

    /**
     * Sends a beautifully formatted payment notification email with the PDF receipt attached.
     */
    public void sendWorkerPaymentWithPdf(String toEmail, String nombre, String periodoDescripcion,
                                          BigDecimal monto, byte[] pdfBytes, String pdfFilename) {
        String subject = "💰 Tu comprobante de pago está listo — Furent";
        String montoStr = monto != null ? "$" + String.format("%,.0f", monto) : "$0";

        String html = buildWorkerPaymentHtmlEmail(nombre, periodoDescripcion, montoStr);
        sendHtmlEmailWithAttachment(toEmail, subject, html, pdfBytes, pdfFilename);
    }

    public void sendWorkerPaymentNotification(String toEmail, String nombre, String periodoDescripcion, BigDecimal monto) {
        String subject = "💰 Tu comprobante de pago está listo — Furent";
        String montoStr = monto != null ? "$" + String.format("%,.0f", monto) : "$0";
        String html = buildWorkerPaymentHtmlEmail(nombre, periodoDescripcion, montoStr);
        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendContactNotification(String adminEmail, String fromName, String subject) {
        String html = buildGenericHtmlEmail(
                "Nuevo Mensaje de Contacto",
                "Has recibido un nuevo mensaje de contacto de <strong>" + fromName + "</strong> con asunto: <strong>" + subject + "</strong>."
        );
        sendHtmlEmail(adminEmail, "Nuevo mensaje de contacto: " + subject, html);
    }

    /**
     * Envía al cliente el código único para pagar en efectivo en oficina.
     */
    public void sendCashPaymentCodeEmail(String toEmail, String nombre, String codigoPago, BigDecimal total, String reservaId) {
        String totalStr = total != null ? "$" + String.format("%,.0f", total) : "$0";
        String subject = "Tu código de pago en efectivo — Furent";
        String body = "Hola, <strong>" + nombre + "</strong>.<br><br>" +
                "Tu reserva <strong>#" + reservaId + "</strong> está registrada con pago en efectivo.<br><br>" +
                "Código para pagar en oficina: <strong style=\"font-size:1.2em;letter-spacing:2px;background:#f0fdf4;padding:8px 16px;border-radius:8px;\">" + codigoPago + "</strong><br><br>" +
                "Total a pagar: <strong>" + totalStr + "</strong><br><br>" +
                "Tienes <strong>48 horas</strong> para acercarte a nuestras instalaciones y realizar el pago con este código. " +
                "Consérvalo y preséntalo al momento de pagar.";
        String html = buildGenericHtmlEmail("Código de pago en efectivo", body);
        sendHtmlEmail(toEmail, subject, html);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL] ✅ Correo enviado a: {} | Asunto: {}", to, subject);
        } catch (MessagingException e) {
            log.error("[EMAIL] ❌ Error enviando correo a: {} | Asunto: {} | Error: {}", to, subject, e.getMessage());
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    private void sendHtmlEmailWithAttachment(String to, String subject, String htmlBody,
                                              byte[] attachment, String attachmentFilename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachment), "application/pdf");
            mailSender.send(message);
            log.info("[EMAIL] ✅ Correo con adjunto enviado a: {} | Asunto: {} | Archivo: {}", to, subject, attachmentFilename);
        } catch (MessagingException e) {
            log.error("[EMAIL] ❌ Error enviando correo con adjunto a: {} | Error: {}", to, e.getMessage());
            throw new RuntimeException("Error al enviar correo con PDF adjunto: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a generic branded HTML email template.
     */
    private String buildGenericHtmlEmail(String title, String bodyContent) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f1fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1fa;padding:32px 0;">
                    <tr><td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(124,58,237,0.08);">
                            <!-- Header -->
                            <tr>
                                <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);padding:28px 32px;text-align:center;">
                                    <h1 style="margin:0;font-size:24px;color:#ffffff;letter-spacing:1px;">FURENT</h1>
                                    <p style="margin:4px 0 0;font-size:11px;color:rgba(255,255,255,0.75);letter-spacing:0.5px;">Alquiler de Mobiliarios para Eventos</p>
                                </td>
                            </tr>
                            <!-- Body -->
                            <tr>
                                <td style="padding:32px;">
                                    <h2 style="margin:0 0 16px;font-size:20px;color:#1f2937;">%s</h2>
                                    <p style="margin:0;font-size:14px;line-height:1.7;color:#4b5563;">%s</p>
                                </td>
                            </tr>
                            <!-- Footer -->
                            <tr>
                                <td style="padding:20px 32px;background-color:#faf5ff;border-top:1px solid #ede9fe;text-align:center;">
                                    <p style="margin:0;font-size:11px;color:#9ca3af;">Este correo fue enviado automáticamente desde Furent.</p>
                                    <p style="margin:4px 0 0;font-size:11px;color:#9ca3af;">© 2026 Furent — Todos los derechos reservados.</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
                </body>
                </html>
                """.formatted(title, bodyContent);
    }

    /**
     * Builds a beautifully formatted HTML email for worker payment notifications.
     */
    private String buildWorkerPaymentHtmlEmail(String nombre, String periodo, String montoStr) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f4f1fa;font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f1fa;padding:32px 0;">
                    <tr><td align="center">
                        <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(124,58,237,0.08);">
                
                            <!-- Gradient Header -->
                            <tr>
                                <td style="background:linear-gradient(135deg,#7c3aed 0%%,#a855f7 100%%);padding:28px 32px;text-align:center;">
                                    <h1 style="margin:0;font-size:24px;color:#ffffff;letter-spacing:1px;">FURENT</h1>
                                    <p style="margin:4px 0 0;font-size:11px;color:rgba(255,255,255,0.75);letter-spacing:0.5px;">Alquiler de Mobiliarios para Eventos</p>
                                </td>
                            </tr>
                
                            <!-- Greeting -->
                            <tr>
                                <td style="padding:32px 32px 8px;">
                                    <h2 style="margin:0;font-size:20px;color:#1f2937;">¡Hola, %s! 👋</h2>
                                    <p style="margin:12px 0 0;font-size:14px;line-height:1.7;color:#4b5563;">
                                        Te informamos que tu comprobante de pago ha sido generado exitosamente.
                                        A continuación encontrarás el resumen:
                                    </p>
                                </td>
                            </tr>
                
                            <!-- Payment Summary Card -->
                            <tr>
                                <td style="padding:16px 32px;">
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background:linear-gradient(135deg,#faf5ff 0%%,#ede9fe 100%%);border-radius:12px;border:1px solid #e9d5ff;">
                                        <tr>
                                            <td style="padding:24px;">
                                                <table width="100%%" cellpadding="0" cellspacing="0">
                                                    <tr>
                                                        <td style="font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:1px;font-weight:bold;padding-bottom:6px;">
                                                            Período
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="font-size:15px;color:#1f2937;font-weight:bold;padding-bottom:16px;">
                                                            %s
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="border-top:1px solid #d8b4fe;padding-top:16px;">
                                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                                <tr>
                                                                    <td style="font-size:12px;color:#6b7280;text-transform:uppercase;letter-spacing:1px;font-weight:bold;">
                                                                        Monto Total
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="font-size:28px;color:#7c3aed;font-weight:bold;padding-top:4px;">
                                                                        %s
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                
                            <!-- PDF Notice -->
                            <tr>
                                <td style="padding:8px 32px 24px;">
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0fdf4;border-radius:10px;border:1px solid #bbf7d0;">
                                        <tr>
                                            <td style="padding:16px 20px;">
                                                <p style="margin:0;font-size:13px;color:#166534;">
                                                    📎 <strong>Tu comprobante en PDF</strong> se encuentra adjunto a este correo.
                                                    Guárdalo para tus registros.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                
                            <!-- Help Text -->
                            <tr>
                                <td style="padding:0 32px 28px;">
                                    <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.6;">
                                        Si tienes alguna duda sobre este pago, no dudes en comunicarte con tu supervisor
                                        o responder directamente a este correo. Estamos para ayudarte.
                                    </p>
                                </td>
                            </tr>
                
                            <!-- Footer -->
                            <tr>
                                <td style="padding:20px 32px;background-color:#faf5ff;border-top:1px solid #ede9fe;text-align:center;">
                                    <p style="margin:0;font-size:12px;font-weight:bold;color:#7c3aed;">Equipo Furent 💜</p>
                                    <p style="margin:8px 0 0;font-size:10px;color:#9ca3af;">
                                        Este correo fue enviado automáticamente desde el sistema de pagos de Furent.
                                    </p>
                                    <p style="margin:2px 0 0;font-size:10px;color:#9ca3af;">© 2026 Furent — Todos los derechos reservados.</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
                </body>
                </html>
                """.formatted(nombre, periodo, montoStr);
    }
}
