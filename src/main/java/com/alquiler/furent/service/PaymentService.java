package com.alquiler.furent.service;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PaymentRepository;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Servicio de gestión de pagos.
 * Controla el ciclo de vida de pagos: inicialización, confirmación y rechazo.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final MetricsConfig metricsConfig;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationService reservationService,
                          NotificationService notificationService,
                          EmailService emailService,
                          MetricsConfig metricsConfig,
                          AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.metricsConfig = metricsConfig;
        this.auditLogService = auditLogService;
    }

    /**
     * Inicia un pago para una reserva confirmada.
     * Genera referencia única PAY-XXXXXXXX y crea notificación al usuario.
     *
     * @param reservaId ID de la reserva
     * @param userId ID del usuario
     * @param metodo Método de pago (EFECTIVO, TRANSFERENCIA, TARJETA)
     * @return Payment creado con estado PENDIENTE
     * @throws InvalidOperationException si la reserva no está CONFIRMADA
     */
    public Payment initPayment(String reservaId, String userId, String metodo) {
        // Validar que la reserva existe y está en estado CONFIRMADA
        Reservation reserva = reservationService.getByIdOrThrow(reservaId);
        
        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            throw new InvalidOperationException(
                "No se puede iniciar el pago. La reserva debe estar en estado CONFIRMADA (estado actual: " + reserva.getEstado() + ")"
            );
        }

        // Crear el pago
        Payment payment = new Payment();
        payment.setTenantId(TenantContext.getCurrentTenant());
        payment.setReservaId(reservaId);
        payment.setUsuarioId(userId);
        payment.setMonto(reserva.getTotal());
        payment.setMetodoPago(metodo);
        payment.setEstado("PENDIENTE");
        payment.setReferencia(generateReference());

        Payment saved = paymentRepository.save(payment);
        log.info("Pago iniciado: {} para reserva {} por usuario {}", saved.getReferencia(), reservaId, userId);

        // Incrementar métrica de pagos creados
        metricsConfig.getPaymentsCreated().increment();

        // Crear notificación para el usuario
        notificationService.notify(
            userId,
            "Pago iniciado",
            "Se ha iniciado el proceso de pago para tu reserva. Referencia: " + saved.getReferencia(),
            "PAGO",
            "/mis-reservas/" + reservaId
        );

        return saved;
    }

    /**
     * Genera una referencia única de pago.
     * Formato: PAY-XXXXXXXX (8 caracteres alfanuméricos)
     *
     * @return Referencia única
     */
    public String generateReference() {
        StringBuilder sb = new StringBuilder("PAY-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Obtiene un pago por ID o lanza excepción.
     *
     * @param id ID del pago
     * @return Payment encontrado
     * @throws ResourceNotFoundException si el pago no existe
     */
    public Payment getByIdOrThrow(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
    }

    /**
     * Confirma un pago pendiente (acción de admin).
     * Actualiza estado a PAGADO, cambia reserva a ENTREGADA, incrementa métricas y envía email.
     *
     * @param paymentId ID del pago
     * @param referencia Referencia de pago externa (comprobante)
     * @param admin Email del admin que confirma
     * @return Payment actualizado con estado PAGADO
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidOperationException si el pago ya fue procesado
     */
    public Payment confirmPayment(String paymentId, String referencia, String admin) {
        Payment payment = getByIdOrThrow(paymentId);

        // Validar que el pago esté en estado PENDIENTE
        if (!"PENDIENTE".equals(payment.getEstado())) {
            throw new InvalidOperationException(
                "No se puede confirmar el pago. El pago ya fue procesado (estado actual: " + payment.getEstado() + ")"
            );
        }

        // Actualizar estado del pago
        payment.setEstado("PAGADO");
        payment.setFechaPago(LocalDateTime.now());
        if (referencia != null && !referencia.isBlank()) {
            payment.setReferencia(referencia);
        }
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Pago {} confirmado por admin {}", paymentId, admin);

        // Cambiar estado de la reserva a ENTREGADA
        try {
            reservationService.updateStatus(payment.getReservaId(), "ENTREGADA", admin, 
                "Pago confirmado - Referencia: " + payment.getReferencia());
        } catch (Exception e) {
            log.error("Error al actualizar estado de reserva {} a ENTREGADA: {}", payment.getReservaId(), e.getMessage());
        }

        // Incrementar métrica de pagos completados
        metricsConfig.getPaymentsCompleted().increment();

        // Acumular ingresos
        if (payment.getMonto() != null) {
            metricsConfig.addRevenue(payment.getMonto());
        }

        // Enviar email de confirmación al usuario
        try {
            emailService.sendPaymentConfirmation(savedPayment);
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación de pago: {}", e.getMessage());
        }

        // Crear notificación para el usuario
        notificationService.notify(
            payment.getUsuarioId(),
            "Pago confirmado",
            "Tu pago ha sido confirmado. Referencia: " + payment.getReferencia(),
            "PAGO",
            "/mis-reservas/" + payment.getReservaId()
        );

        // Registrar en auditoría
        auditLogService.log(
            admin,
            "CONFIRMAR_PAGO",
            "PAGO",
            paymentId,
            "Pago confirmado - Referencia: " + payment.getReferencia() + " - Monto: " + payment.getMonto()
        );

        return savedPayment;
    }

    /**
     * Rechaza un pago pendiente (acción de admin).
     * Actualiza estado a FALLIDO e incrementa métrica de pagos fallidos.
     *
     * @param paymentId ID del pago
     * @param reason Razón del rechazo
     * @param admin Email del admin que rechaza
     * @return Payment actualizado con estado FALLIDO
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidOperationException si el pago ya fue procesado
     */
    public Payment failPayment(String paymentId, String reason, String admin) {
        Payment payment = getByIdOrThrow(paymentId);

        // Validar que el pago esté en estado PENDIENTE
        if (!"PENDIENTE".equals(payment.getEstado())) {
            throw new InvalidOperationException(
                "No se puede rechazar el pago. El pago ya fue procesado (estado actual: " + payment.getEstado() + ")"
            );
        }

        // Actualizar estado del pago
        payment.setEstado("FALLIDO");
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Pago {} rechazado por admin {}. Razón: {}", paymentId, admin, reason);

        // Incrementar métrica de pagos fallidos
        metricsConfig.getPaymentsFailed().increment();

        // Crear notificación para el usuario
        notificationService.notify(
            payment.getUsuarioId(),
            "Pago rechazado",
            "Tu pago ha sido rechazado. Razón: " + (reason != null ? reason : "No especificada"),
            "PAGO",
            "/mis-reservas/" + payment.getReservaId()
        );

        // Registrar en auditoría
        auditLogService.log(
            admin,
            "RECHAZAR_PAGO",
            "PAGO",
            paymentId,
            "Pago rechazado - Razón: " + (reason != null ? reason : "No especificada") + " - Monto: " + payment.getMonto()
        );

        return savedPayment;
    }

    // ========== Métodos auxiliares (serán implementados en tareas futuras) ==========

    /**
     * Obtiene todos los pagos del sistema.
     */
    public java.util.List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Obtiene todos los pagos del sistema con paginación.
     * Usado por el panel de administración.
     *
     * @param pageable Configuración de paginación
     * @return Page de pagos
     */
    public org.springframework.data.domain.Page<Payment> getAllPaymentsPaged(org.springframework.data.domain.Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    /**
     * Obtiene todos los pagos de un usuario.
     *
     * @param userId ID del usuario
     * @return Lista de pagos del usuario
     */
    public java.util.List<Payment> getPaymentsByUser(String userId) {
        return paymentRepository.findByUsuarioId(userId);
    }

    /**
     * Obtiene el pago asociado a una reserva.
     *
     * @param reservaId ID de la reserva
     * @return Optional con el pago si existe
     */
    public java.util.Optional<Payment> getPaymentByReserva(String reservaId) {
        java.util.List<Payment> payments = paymentRepository.findByReservaId(reservaId);
        return payments.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(payments.get(0));
    }

    /**
     * Confirma un pago usando su referencia.
     * Usado por webhooks de pasarelas de pago (PayU, etc.)
     *
     * @param referencia Referencia del pago
     * @param transactionId ID de transacción externa
     * @param admin Usuario que confirma (puede ser "SYSTEM_PAYU" para webhooks)
     * @return Payment confirmado
     * @throws ResourceNotFoundException si no existe pago con esa referencia
     */
    public Payment confirmPaymentByReference(String referencia, String transactionId, String admin) {
        Payment payment = paymentRepository.findByReferencia(referencia)
                .orElseThrow(() -> new ResourceNotFoundException("Pago con referencia", referencia));
        return confirmPayment(payment.getId(), transactionId, admin);
    }
}
