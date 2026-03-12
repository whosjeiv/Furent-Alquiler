package com.alquiler.furent.service;

import com.alquiler.furent.enums.EstadoPago;
import com.alquiler.furent.enums.EstadoReserva;
import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PaymentRepository;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de gestión de pagos.
 * Maneja el flujo completo de pagos: iniciación, confirmación y rechazo,
 * con notificaciones automáticas al usuario y registro de auditoría.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final EventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;

    public PaymentService(PaymentRepository paymentRepository, ReservationService reservationService,
                          NotificationService notificationService, AuditLogService auditLogService,
                          EventPublisher eventPublisher, MetricsConfig metricsConfig) {
        this.paymentRepository = paymentRepository;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
    }

    public Payment initPayment(String reservaId, String usuarioId, String metodoPago) {
        Reservation reserva = reservationService.getById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        Payment payment = new Payment();
        payment.setReservaId(reservaId);
        payment.setUsuarioId(usuarioId);
        payment.setMonto(reserva.getTotal());
        payment.setMetodoPago(metodoPago);
        payment.setEstado(EstadoPago.PENDIENTE.name());

        Payment saved = paymentRepository.save(payment);
        log.info("Pago iniciado: {} para reserva: {}", saved.getId(), reservaId);

        notificationService.notify(usuarioId, "Pago Iniciado",
                "Tu pago por $" + String.format("%,.0f", reserva.getTotal()) + " está pendiente de confirmación.",
                "INFO", "/panel");

        return saved;
    }

    public Payment confirmPayment(String paymentId, String referencia, String adminUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", paymentId));

        if (!EstadoPago.PENDIENTE.name().equals(payment.getEstado())) {
            throw new InvalidOperationException("Solo se pueden confirmar pagos pendientes");
        }

        payment.setEstado(EstadoPago.PAGADO.name());
        payment.setReferencia(referencia);
        payment.setFechaPago(LocalDateTime.now());

        long start = System.nanoTime();
        Payment saved = paymentRepository.save(payment);
        metricsConfig.getPaymentProcessingTime()
                .record(java.time.Duration.ofNanos(System.nanoTime() - start));

        // Actualizar estado de reserva
        reservationService.updateStatus(payment.getReservaId(), EstadoReserva.ACTIVA.name());

        // Métricas: pago confirmado + ingresos
        metricsConfig.getPaymentsCompleted().increment();
        if (saved.getMonto() != null) {
            metricsConfig.addRevenue(saved.getMonto());
        }

        // Notificar al usuario
        notificationService.notify(payment.getUsuarioId(), "Pago Confirmado",
                "Tu pago ha sido confirmado exitosamente. Tu reserva está activa.",
                "SUCCESS", "/panel");

        auditLogService.log(adminUser, "CONFIRMAR_PAGO", "PAGO", paymentId,
                "Pago confirmado. Ref: " + referencia);

        log.info("Pago confirmado: {} por admin: {}", paymentId, adminUser);

        // Publicar evento
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        eventPublisher.publish(new PaymentCompletedEvent(this, saved, tenantId));

        return saved;
    }

    public Payment failPayment(String paymentId, String reason, String adminUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", paymentId));

        payment.setEstado(EstadoPago.FALLIDO.name());
        Payment saved = paymentRepository.save(payment);
        metricsConfig.getPaymentsFailed().increment();

        notificationService.notify(payment.getUsuarioId(), "Pago Rechazado",
                "Tu pago fue rechazado: " + reason + ". Por favor, intenta de nuevo.",
                "ALERT", "/pago/iniciar/" + payment.getReservaId());

        auditLogService.log(adminUser, "RECHAZAR_PAGO", "PAGO", paymentId, "Razón: " + reason);
        log.warn("Pago rechazado: {} razón: {}", paymentId, reason);
        return saved;
    }

    public List<Payment> getPaymentsByUser(String userId) {
        return paymentRepository.findByUsuarioId(userId);
    }

    public Optional<Payment> getPaymentByReserva(String reservaId) {
        List<Payment> payments = paymentRepository.findByReservaId(reservaId);
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public Optional<Payment> getById(String id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
}
