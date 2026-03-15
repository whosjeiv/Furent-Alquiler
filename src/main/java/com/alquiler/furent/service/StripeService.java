package com.alquiler.furent.service;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.config.StripeProperties;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.enums.EstadoPago;
import com.alquiler.furent.enums.EstadoReserva;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.PaymentCompletedEvent;
import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.PendingCardPayment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PaymentRepository;
import com.alquiler.furent.repository.PendingCardPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de integración con Stripe para pagos con tarjeta.
 * Crea PaymentIntents y procesa webhooks para confirmar pagos automáticamente.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final StripeProperties stripeProperties;
    private final PaymentRepository paymentRepository;
    private final PendingCardPaymentRepository pendingCardPaymentRepository;
    private final ReservationService reservationService;
    private final com.alquiler.furent.service.CouponService couponService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final EventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;

    public StripeService(StripeProperties stripeProperties,
                         PaymentRepository paymentRepository,
                         PendingCardPaymentRepository pendingCardPaymentRepository,
                         @Lazy ReservationService reservationService,
                         com.alquiler.furent.service.CouponService couponService,
                         NotificationService notificationService,
                         AuditLogService auditLogService,
                         EventPublisher eventPublisher,
                         MetricsConfig metricsConfig) {
        this.stripeProperties = stripeProperties;
        this.paymentRepository = paymentRepository;
        this.pendingCardPaymentRepository = pendingCardPaymentRepository;
        this.reservationService = reservationService;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
    }

    /**
     * Resultado de crear un PaymentIntent: clientSecret para el frontend e id para persistir en el pago.
     */
    public static final class PaymentIntentResult {
        private final String clientSecret;
        private final String paymentIntentId;

        public PaymentIntentResult(String clientSecret, String paymentIntentId) {
            this.clientSecret = clientSecret;
            this.paymentIntentId = paymentIntentId;
        }

        public String getClientSecret() { return clientSecret; }
        public String getPaymentIntentId() { return paymentIntentId; }
    }

    /**
     * Crea un PaymentIntent en Stripe para el pago indicado.
     * @param payment Pago ya persistido (con id y reservaId).
     * @return clientSecret e id del intent para guardar en el pago, o empty si Stripe no está configurado.
     */
    public Optional<PaymentIntentResult> createPaymentIntent(Payment payment) {
        if (!stripeProperties.isConfigured()) {
            return Optional.empty();
        }
        if (payment.getMonto() == null || payment.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripeProperties.getSecretKey())
                    .build();
            long amountCents = payment.getMonto().multiply(BigDecimal.valueOf(100)).longValue();
            if (amountCents < 50) {
                amountCents = 50; // Stripe mínimo
            }
            Map<String, String> metadata = new HashMap<>();
            metadata.put("paymentId", payment.getId());
            metadata.put("reservaId", payment.getReservaId());
            metadata.put("usuarioId", payment.getUsuarioId() != null ? payment.getUsuarioId() : "");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(stripeProperties.getCurrency())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, requestOptions);
            log.info("Stripe PaymentIntent creado: {} para pago {}", intent.getId(), payment.getId());
            return Optional.of(new PaymentIntentResult(intent.getClientSecret(), intent.getId()));
        } catch (StripeException e) {
            log.error("Error creando PaymentIntent para pago {}: {}", payment.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Crea un PaymentIntent para una cotización pendiente (sin reserva aún).
     * La reserva se crea cuando el webhook confirma el pago.
     */
    public Optional<PaymentIntentResult> createPaymentIntentForPending(PendingCardPayment pending, BigDecimal amount) {
        if (!stripeProperties.isConfigured() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripeProperties.getSecretKey())
                    .build();
            long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            if (amountCents < 50) amountCents = 50;
            Map<String, String> metadata = new HashMap<>();
            metadata.put("pendingId", pending.getId());
            metadata.put("usuarioId", pending.getUsuarioId() != null ? pending.getUsuarioId() : "");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(stripeProperties.getCurrency())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, requestOptions);
            pending.setStripePaymentIntentId(intent.getId());
            pendingCardPaymentRepository.save(pending);
            log.info("Stripe PaymentIntent creado para pending {}: {}", pending.getId(), intent.getId());
            return Optional.of(new PaymentIntentResult(intent.getClientSecret(), intent.getId()));
        } catch (StripeException e) {
            log.error("Error creando PaymentIntent para pending {}: {}", pending.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Obtiene el clientSecret de un PaymentIntent ya creado (para la página de pago con pendingId).
     */
    public Optional<String> retrieveClientSecret(String paymentIntentId) {
        if (!stripeProperties.isConfigured()) return Optional.empty();
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripeProperties.getSecretKey())
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, requestOptions);
            return Optional.ofNullable(intent.getClientSecret());
        } catch (StripeException e) {
            log.error("Error recuperando PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Procesa un evento de webhook de Stripe (payload crudo y cabecera de firma).
     * En payment_intent.succeeded marca el pago como PAGADO y la reserva como ENTREGADA.
     */
    public boolean handleWebhookEvent(String payload, String stripeSignatureHeader) {
        if (!stripeProperties.isConfigured() || stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            log.warn("Webhook Stripe recibido pero Stripe no está configurado o falta webhook secret");
            return false;
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Firma de webhook Stripe inválida: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error parseando webhook Stripe: {}", e.getMessage());
            return false;
        }

        if (!"payment_intent.succeeded".equals(event.getType())) {
            log.debug("Webhook Stripe ignorado (tipo {}): {}", event.getType(), event.getId());
            return true;
        }

        String paymentIntentId;
        try {
            JsonNode root = new ObjectMapper().readTree(payload);
            paymentIntentId = root.path("data").path("object").path("id").asText();
        } catch (Exception e) {
            log.error("Error extrayendo id de PaymentIntent del webhook: {}", e.getMessage());
            return false;
        }
        Optional<Payment> optPayment = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (optPayment.isPresent()) {
            Payment payment = optPayment.get();
            if (EstadoPago.PAGADO.name().equals(payment.getEstado())) {
                log.info("Pago {} ya estaba PAGADO (idempotencia webhook)", payment.getId());
                return true;
            }
            payment.setEstado(EstadoPago.PAGADO.name());
            payment.setReferencia("Stripe: " + paymentIntentId);
            payment.setFechaPago(LocalDateTime.now());
            paymentRepository.save(payment);
            reservationService.updateStatus(payment.getReservaId(), EstadoReserva.ENTREGADA.name());
            metricsConfig.getPaymentsCompleted().increment();
            if (payment.getMonto() != null) metricsConfig.addRevenue(payment.getMonto());
            notificationService.notify(payment.getUsuarioId(), "Pago Confirmado",
                    "Tu pago con tarjeta ha sido procesado correctamente. Tu reserva está activa.", "SUCCESS", "/panel");
            auditLogService.log("stripe-webhook", "CONFIRMAR_PAGO_STRIPE", "PAGO", payment.getId(),
                    "Pago confirmado vía Stripe. PaymentIntent: " + paymentIntentId);
            try {
                eventPublisher.publish(new PaymentCompletedEvent(this, payment,
                        TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default"));
            } catch (Exception e) { log.warn("Error publicando PaymentCompletedEvent: {}", e.getMessage()); }
            log.info("Pago {} confirmado por webhook Stripe. Reserva {} → ENTREGADA.", payment.getId(), payment.getReservaId());
            return true;
        }

        Optional<PendingCardPayment> optPending = pendingCardPaymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (optPending.isEmpty()) {
            log.warn("Webhook payment_intent.succeeded para {} sin Payment ni PendingCardPayment", paymentIntentId);
            return true;
        }
        PendingCardPayment pending = optPending.get();
        try {
            Reservation res = OBJECT_MAPPER.readValue(pending.getReservationDataJson(), Reservation.class);
            res.setId(null);
            res.setEstado(EstadoReserva.ENTREGADA.name());
            res.setFechaCreacion(LocalDateTime.now());
            res.setFechaActualizacion(LocalDateTime.now());
            reservationService.save(res);
            if (res.getCodigoCupon() != null && !res.getCodigoCupon().isBlank()) {
                try { couponService.useCoupon(res.getCodigoCupon()); } catch (Exception e) { log.warn("Cupón ya usado o inválido: {}", e.getMessage()); }
            }
            Payment payment = new Payment();
            payment.setReservaId(res.getId());
            payment.setUsuarioId(res.getUsuarioId());
            payment.setTenantId(res.getTenantId());
            payment.setMonto(res.getTotal());
            payment.setMetodoPago("TARJETA");
            payment.setEstado(EstadoPago.PAGADO.name());
            payment.setReferencia("Stripe: " + paymentIntentId);
            payment.setFechaPago(LocalDateTime.now());
            payment.setStripePaymentIntentId(paymentIntentId);
            paymentRepository.save(payment);
            metricsConfig.getPaymentsCompleted().increment();
            if (res.getTotal() != null) metricsConfig.addRevenue(res.getTotal());
            notificationService.notify(res.getUsuarioId(), "Pago Confirmado",
                    "Tu pago con tarjeta ha sido procesado. Tu reserva #" + res.getId() + " está activa.", "SUCCESS", "/panel");
            auditLogService.log("stripe-webhook", "CREAR_RESERVA_STRIPE", "RESERVA", res.getId(),
                    "Reserva creada tras pago con tarjeta. PaymentIntent: " + paymentIntentId);
            try {
                eventPublisher.publish(new PaymentCompletedEvent(this, payment,
                        TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default"));
            } catch (Exception e) { log.warn("Error publicando PaymentCompletedEvent: {}", e.getMessage()); }
            pendingCardPaymentRepository.delete(pending);
            log.info("Reserva {} creada por webhook Stripe (pending {}).", res.getId(), pending.getId());
        } catch (Exception e) {
            log.error("Error creando reserva desde pending {}: {}", pending.getId(), e.getMessage());
            return false;
        }
        return true;
    }
}
