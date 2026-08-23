package com.alquiler.furent.controller;

import com.alquiler.furent.config.PayUProperties;
import com.alquiler.furent.config.FeatureFlags;
import com.alquiler.furent.enums.MetodoPago;
import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import com.alquiler.furent.model.PendingCardPayment;
import com.alquiler.furent.repository.PendingCardPaymentRepository;
import com.alquiler.furent.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final PendingCardPaymentRepository pendingCardPaymentRepository;
    private final FeatureFlags featureFlags;
    private final PayUProperties payUProperties;
    private final CouponService couponService;
    private final EmailService emailService;

    private static final ObjectMapper RESERVATION_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public PaymentController(PaymentService paymentService, ReservationService reservationService,
                             UserService userService, PendingCardPaymentRepository pendingCardPaymentRepository,
                             FeatureFlags featureFlags, PayUProperties payUProperties,
                             CouponService couponService, EmailService emailService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.pendingCardPaymentRepository = pendingCardPaymentRepository;
        this.featureFlags = featureFlags;
        this.payUProperties = payUProperties;
        this.couponService = couponService;
        this.emailService = emailService;
    }

    /**
     * Obtiene clientSecret y datos para un pago pendiente (flujo tarjeta sin reserva previa).
     * El usuario debe ser el dueño del pending.
     */
    @GetMapping("/pending/{pendingId}")
    public ResponseEntity<Map<String, Object>> getPendingPayment(@PathVariable String pendingId, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        java.util.Optional<PendingCardPayment> pendingOpt = pendingCardPaymentRepository.findById(pendingId);
        if (pendingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var pending = pendingOpt.get();
        if (!pending.getUsuarioId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("total", pending.getTotal());
        return ResponseEntity.ok(body);
    }

    /**
     * Pago con tarjeta online (PayU)
     */
    @GetMapping("/payu/config")
    public ResponseEntity<Map<String, Object>> getPayUConfig() {
        boolean enabled = featureFlags.isPayuEnabled() && payUProperties.isConfigured();
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enabled);
        return ResponseEntity.ok(body);
    }

    /**
     * Webhook de confirmación de PayU (Confirmación de Pago).
     * PayU llama a este endpoint por POST con parámetros x_www_form_url_encoded.
     */
    @PostMapping("/payu/confirmacion")
    public ResponseEntity<?> handleConfirmation(@RequestParam Map<String, String> params) {
        log.info("Webhook Pago Recibido: {}", params);

        String referenceCode = params.get("reference_sale");
        String statePolStr = params.get("state_pol");
        String valueStr = params.get("value");
        String sign = params.get("sign");
        // String currency = params.get("currency");

        if (referenceCode == null || statePolStr == null || sign == null) {
            return ResponseEntity.badRequest().build();
        }

        int statePol = Integer.parseInt(statePolStr);
        BigDecimal value = new BigDecimal(valueStr);

        // 1. Validar Firma
        // En un entorno de producción, descomentar para validar el origen
        // String localSign = payUService.generateConfirmationSignature(referenceCode, value, currency, statePol);
        // if (!localSign.equalsIgnoreCase(sign)) {
        //     log.error("Firma inválida de PayU. Local: {}, Remota: {}", localSign, sign);
        //     return ResponseEntity.status(401).body("Firma inválida");
        // }

        // 2. Procesar según estado (4 = Aprobado, 6 = Rechazado)
        if (statePol == 4) {
            processSuccessfulPayment(referenceCode, params.get("transaction_id"), value);
        } else if (statePol == 6) {
            log.warn("Pago rechazado por PayU para referencia: {}", referenceCode);
        }

        return ResponseEntity.ok("OK");
    }

    private void processSuccessfulPayment(String reference, String transactionId, BigDecimal amount) {
        // El reference puede ser un reservaId (pago de reserva existente) o un pendingId (pago nuevo)
        var pendingOpt = pendingCardPaymentRepository.findById(reference);
        if (pendingOpt.isPresent()) {
            PendingCardPayment pending = pendingOpt.get();
            try {
                Reservation res = RESERVATION_MAPPER.readValue(pending.getReservationDataJson(), Reservation.class);
                res.setEstado("CONFIRMADA");
                res.setMetodoPago("TARJETA");
                res.setFechaCreacion(LocalDateTime.now());
                
                // Guardar reserva real
                reservationService.save(res);
                
                // Crear Registro de Pago
                paymentService.confirmPaymentByReference(res.getId(), transactionId, "SYSTEM_PAYU");

                // Consumir cupón si existe
                if (res.getCodigoCupon() != null) {
                    couponService.useCoupon(res.getCodigoCupon());
                }

                // Notificar por email
                emailService.sendReservationConfirmation(res.getUsuarioEmail(), res.getId(), res.getTotal());
                
                // Borrar el pendiente
                pendingCardPaymentRepository.delete(pending);
                
                log.info("Reserva {} creada exitosamente desde pago con tarjeta {}", res.getId(), reference);
            } catch (Exception e) {
                log.error("Error al convertir pendiene a reserva: {}", e.getMessage(), e);
            }
        } else {
            // Es un pago de una reserva que ya existía (pago posterior)
            paymentService.confirmPaymentByReference(reference, transactionId, "SYSTEM_PAYU");
        }
    }

    @PostMapping("/iniciar/{reservaId}")
    public ResponseEntity<Map<String, Object>> initPayment(@PathVariable String reservaId,
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        Reservation reserva = reservationService.getById(reservaId).orElse(null);
        if (reserva == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reserva no encontrada"));
        }

        if (!reserva.getUsuarioId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        String metodoPago = body.getOrDefault("metodoPago", reserva.getMetodoPago());
        if (metodoPago != null && (metodoPago.equals("Credit Card") || metodoPago.equals(MetodoPago.TARJETA.name()))) {
            metodoPago = MetodoPago.TARJETA.name();
        }
        Payment payment = paymentService.initPayment(reservaId, user.getId(), metodoPago);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("paymentId", payment.getId());
        response.put("monto", payment.getMonto());
        response.put("metodoPago", payment.getMetodoPago());

        log.info("Pago iniciado por usuario {} para reserva {}", user.getEmail(), reservaId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<?> getPaymentByReserva(@PathVariable String reservaId, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        return paymentService.getPaymentByReserva(reservaId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of(
                        "id", p.getId(),
                        "estado", p.getEstado(),
                        "monto", p.getMonto(),
                        "metodoPago", p.getMetodoPago(),
                        "fechaPago", p.getFechaPago() != null ? p.getFechaPago().toString() : ""
                )))
                .orElse(ResponseEntity.ok(Map.of("existe", "false")));
    }

    @GetMapping("/mis-pagos")
    public ResponseEntity<?> getMyPayments(Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(paymentService.getPaymentsByUser(user.getId()));
    }

    private User getAuthUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
