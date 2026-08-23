package com.alquiler.furent.event;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.AuditLogService;
import com.alquiler.furent.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener centralizado de eventos del dominio.
 * Reacciona a eventos publicados en el bus interno para:
 * - Enviar notificaciones
 * - Registrar auditoría
 * - Incrementar métricas Prometheus
 * - Acciones secundarias (analytics, emails, etc.)
 *
 * Evolución futura: migrar a Apache Kafka / RabbitMQ para escalar horizontalmente.
 */
@Component
public class FurentEventListener {

    private static final Logger log = LoggerFactory.getLogger(FurentEventListener.class);

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final MetricsConfig metricsConfig;

    public FurentEventListener(NotificationService notificationService,
                               AuditLogService auditLogService,
                               MetricsConfig metricsConfig) {
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.metricsConfig = metricsConfig;
    }

    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        log.info("Evento: UserRegistered - {} [tenant={}]", user.getEmail(), event.getTenantId());

        // Métrica Prometheus
        metricsConfig.getUsersRegistered().increment();

        // Notificación de bienvenida
        notificationService.notify(
                user.getId(),
                "¡Bienvenido a Furent!",
                "Tu cuenta ha sido creada exitosamente. Explora nuestro catálogo de mobiliarios.",
                "BIENVENIDA",
                "/catalogo"
        );

        // Auditoría
        auditLogService.log(user.getEmail(), "REGISTRO", "USER", user.getId(),
                "Nuevo usuario registrado en tenant: " + event.getTenantId());
    }

    @Async
    @EventListener
    public void handleReservationCreated(ReservationCreatedEvent event) {
        Reservation res = event.getReservation();
        log.info("Evento: ReservationCreated - reserva={} [tenant={}]", res.getId(), event.getTenantId());

        // Métrica Prometheus
        metricsConfig.getReservationsCreated().increment();

        // Notificación al usuario
        notificationService.notify(
                res.getUsuarioId(),
                "Reserva Creada",
                "Tu reserva #" + res.getId() + " ha sido creada exitosamente. Estado: " + res.getEstado(),
                "RESERVA",
                "/panel"
        );

        // Auditoría
        auditLogService.log(res.getUsuarioId(), "CREAR_RESERVA", "RESERVATION", res.getId(),
                "Reserva creada - Total: $" + res.getTotal());
    }

    @Async
    @EventListener
    public void handleReservationCancelled(ReservationCancelledEvent event) {
        Reservation res = event.getReservation();
        log.info("Evento: ReservationCancelled - reserva={} [tenant={}]", res.getId(), event.getTenantId());

        // Métrica Prometheus
        metricsConfig.getReservationsCancelled().increment();

        notificationService.notify(
                res.getUsuarioId(),
                "Reserva Cancelada",
                "Tu reserva #" + res.getId() + " ha sido cancelada. Razón: " + event.getReason(),
                "RESERVA",
                "/panel"
        );

        auditLogService.log(res.getUsuarioId(), "CANCELAR_RESERVA", "RESERVATION", res.getId(),
                "Reserva cancelada - Razón: " + event.getReason());
    }

    @Async
    @EventListener
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        var payment = event.getPayment();
        log.info("Evento: PaymentCompleted - pago={} [tenant={}]", payment.getId(), event.getTenantId());

        // Métrica Prometheus
        metricsConfig.getPaymentsCompleted().increment();

        notificationService.notify(
                payment.getUsuarioId(),
                "Pago Confirmado",
                "Tu pago de $" + payment.getMonto() + " ha sido procesado exitosamente.",
                "PAGO",
                "/panel"
        );

        auditLogService.log(payment.getUsuarioId(), "PAGO_COMPLETADO", "PAYMENT", payment.getId(),
                "Pago completado - Método: " + payment.getMetodoPago() + " - Monto: $" + payment.getMonto());
    }

    @Async
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        var product = event.getProduct();
        log.info("Evento: ProductUpdated - producto={} accion={} [tenant={}]",
                product.getId(), event.getAction(), event.getTenantId());

        auditLogService.log("SYSTEM", event.getAction() + "_PRODUCTO", "PRODUCT", product.getId(),
                "Producto " + event.getAction().toLowerCase() + ": " + product.getNombre());
    }

    @Async
    @EventListener
    public void handleReviewCreated(ReviewCreatedEvent event) {
        Review review = event.getReview();
        log.info("Evento: ReviewCreated - review={} [tenant={}]", review.getId(), event.getTenantId());

        // Métrica Prometheus
        metricsConfig.getReviewsCreated().increment();

        auditLogService.log(review.getUserId(), "CREAR_REVIEW", "REVIEW", review.getId(),
                "Review creada para producto: " + review.getProductId() + " - Rating: " + review.getRating());
    }
}
