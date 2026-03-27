package com.alquiler.furent.service;

import com.alquiler.furent.enums.EstadoPago;
import com.alquiler.furent.enums.EstadoReserva;
import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PaymentRepository;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        return confirmPaymentInternal(payment, referencia, adminUser);
    }

    public void confirmPaymentByReference(String reservaId, String referencia, String adminUser) {
        Optional<Payment> paymentOpt = getPaymentByReserva(reservaId);
        if (paymentOpt.isPresent()) {
            confirmPaymentInternal(paymentOpt.get(), referencia, adminUser);
        } else {
            // Si no existe el pago, lo inicializamos y confirmamos
            Reservation res = reservationService.getById(reservaId).orElse(null);
            if (res != null) {
                Payment p = initPayment(reservaId, res.getUsuarioId(), "TARJETA");
                confirmPaymentInternal(p, referencia, adminUser);
            }
        }
    }

    private Payment confirmPaymentInternal(Payment payment, String referencia, String adminUser) {
        if (!EstadoPago.PENDIENTE.name().equals(payment.getEstado())) {
            return payment;
        }

        payment.setEstado(EstadoPago.PAGADO.name());
        payment.setReferencia(referencia);
        payment.setFechaPago(LocalDateTime.now());

        long start = System.nanoTime();
        Payment saved = paymentRepository.save(payment);
        if (metricsConfig != null) {
             metricsConfig.getPaymentProcessingTime()
                .record(java.time.Duration.ofNanos(System.nanoTime() - start));
             metricsConfig.getPaymentsCompleted().increment();
             if (saved.getMonto() != null) {
                 metricsConfig.addRevenue(saved.getMonto());
             }
        }

        // Actualizar estado de reserva
        reservationService.updateStatus(payment.getReservaId(), EstadoReserva.CONFIRMADA.name());

        // Notificar al usuario
        notificationService.notify(payment.getUsuarioId(), "Pago Confirmado",
                "Tu pago ha sido confirmado exitosamente. Tu reserva está activa.",
                "SUCCESS", "/panel");

        auditLogService.log(adminUser, "CONFIRMAR_PAGO", "PAGO", payment.getId(),
                "Pago confirmado. Ref: " + referencia);

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

    /**
     * Registra un abono (pago parcial) a una reserva.
     * Valida que el monto no exceda el saldo pendiente, crea el registro de pago,
     * y actualiza el estado financiero de la reserva.
     */
    public Payment registrarAbono(String reservaId, BigDecimal monto, String tipoPago,
                                   String referencia, String nota, String adminUser) {
        Reservation reserva = reservationService.getById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.alquiler.furent.exception.InvalidOperationException("El monto del abono debe ser mayor a cero.");
        }

        BigDecimal saldoPendiente = reserva.getSaldoPendiente();
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new com.alquiler.furent.exception.InvalidOperationException(
                    String.format("El monto ($%,.0f) excede el saldo pendiente ($%,.0f).", monto, saldoPendiente));
        }

        // Crear registro de pago
        Payment payment = new Payment();
        payment.setReservaId(reservaId);
        payment.setUsuarioId(reserva.getUsuarioId());
        payment.setMonto(monto);
        payment.setMetodoPago(reserva.getMetodoPago() != null ? reserva.getMetodoPago() : "EFECTIVO");
        payment.setEstado(EstadoPago.PAGADO.name());
        payment.setTipoPago(tipoPago != null ? tipoPago : "ABONO");
        payment.setReferencia(referencia);
        payment.setNota(nota);
        payment.setFechaPago(LocalDateTime.now());
        if (TenantContext.getCurrentTenant() != null) {
            payment.setTenantId(TenantContext.getCurrentTenant());
        }

        Payment saved = paymentRepository.save(payment);

        // Actualizar monto abonado en la reserva
        BigDecimal nuevoAbonado = (reserva.getMontoAbonado() != null ? reserva.getMontoAbonado() : BigDecimal.ZERO).add(monto);
        reserva.setMontoAbonado(nuevoAbonado);

        // Determinar estado financiero
        BigDecimal totalReserva = reserva.getTotal() != null ? reserva.getTotal() : BigDecimal.ZERO;
        if (nuevoAbonado.compareTo(totalReserva) >= 0) {
            reserva.setEstadoPago("PAGADO");
        } else if (nuevoAbonado.compareTo(BigDecimal.ZERO) > 0) {
            // Primer pago = ANTICIPO, pagos subsiguientes = PARCIAL
            reserva.setEstadoPago("ANTICIPO".equals(tipoPago) || reserva.getPorcentajePagado() <= 50 ? "ANTICIPO" : "PARCIAL");
        }
        reserva.setFechaActualizacion(LocalDateTime.now());
        reservationService.getById(reservaId).ifPresent(r -> {
            r.setMontoAbonado(nuevoAbonado);
            r.setEstadoPago(reserva.getEstadoPago());
            r.setFechaActualizacion(LocalDateTime.now());
            reservationService.save(r);
        });

        // Notificar al usuario
        notificationService.notify(reserva.getUsuarioId(), "Abono Registrado",
                String.format("Se registró un abono de $%,.0f a tu reserva. Saldo pendiente: $%,.0f",
                        monto, totalReserva.subtract(nuevoAbonado).max(BigDecimal.ZERO)),
                "SUCCESS", "/panel");

        auditLogService.log(adminUser, "REGISTRAR_ABONO", "PAGO", saved.getId(),
                String.format("Abono $%,.0f (%s) — Ref: %s", monto, tipoPago, referencia));

        log.info("Abono registrado: {} de ${} para reserva: {} por {}", saved.getId(), monto, reservaId, adminUser);

        return saved;
    }

    /**
     * Obtiene todos los pagos asociados a una reserva.
     */
    public List<Payment> getPaymentsByReserva(String reservaId) {
        return paymentRepository.findByReservaId(reservaId);
    }

    /**
     * Calcula el total abonado (pagados) para una reserva.
     */
    public java.math.BigDecimal getTotalAbonado(String reservaId) {
        return paymentRepository.findByReservaId(reservaId).stream()
                .filter(p -> EstadoPago.PAGADO.name().equals(p.getEstado()))
                .map(Payment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
