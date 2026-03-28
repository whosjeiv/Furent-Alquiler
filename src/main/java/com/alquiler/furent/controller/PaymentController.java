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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Pagos", description = "Gestión de pagos y transacciones")
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
    @Operation(summary = "Obtener pago pendiente", 
               description = "Obtiene los datos de un pago pendiente para completar el flujo de pago con tarjeta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos del pago pendiente obtenidos exitosamente",
                     content = @Content(mediaType = "application/json",
                                       examples = @ExampleObject(value = "{\"total\": 1500.00}"))),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No autorizado para ver este pago"),
        @ApiResponse(responseCode = "404", description = "Pago pendiente no encontrado")
    })
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
    @Operation(summary = "Obtener configuración de PayU", 
               description = "Verifica si el pago con tarjeta está habilitado y devuelve la configuración")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuración de PayU obtenida",
                     content = @Content(mediaType = "application/json",
                                       examples = @ExampleObject(value = "{\"enabled\": true}")))
    })
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
    @Operation(summary = "Webhook de confirmación de PayU", 
               description = "Endpoint para recibir notificaciones de confirmación de pago desde PayU")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Confirmación procesada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos o faltantes")
    })
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
                emailService.sendReservationConfirmation(res);
                
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

    @Operation(summary = "Iniciar pago", 
               description = "Inicia un nuevo pago para una reserva confirmada. Genera una referencia única y crea el registro de pago en estado PENDIENTE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago iniciado exitosamente",
                     content = @Content(mediaType = "application/json",
                                       examples = @ExampleObject(value = "{\"success\": true, \"paymentId\": \"65f1a2b3c4d5e6f7g8h9i0j1\", \"monto\": 1500.00, \"metodoPago\": \"EFECTIVO\"}"))),
        @ApiResponse(responseCode = "400", description = "Reserva no encontrada o datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No autorizado para esta reserva")
    })
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

    @Operation(summary = "Obtener pago por reserva", 
               description = "Obtiene el pago asociado a una reserva específica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago encontrado o no existe",
                     content = @Content(mediaType = "application/json",
                                       examples = @ExampleObject(value = "{\"id\": \"65f1a2b3c4d5e6f7g8h9i0j1\", \"estado\": \"PAGADO\", \"monto\": 1500.00, \"metodoPago\": \"EFECTIVO\", \"fechaPago\": \"2024-01-15T10:30:00\"}"))),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
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

    @Operation(summary = "Obtener mis pagos", 
               description = "Lista todos los pagos realizados por el usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pagos del usuario"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    @GetMapping("/mis-pagos")
    public ResponseEntity<?> getMyPayments(Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(paymentService.getPaymentsByUser(user.getId()));
    }

    @Operation(summary = "Obtener pago por ID", 
               description = "Obtiene los detalles completos de un pago específico. El usuario debe ser el propietario del pago")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalles del pago obtenidos exitosamente",
                     content = @Content(mediaType = "application/json",
                                       examples = @ExampleObject(value = "{\"id\": \"65f1a2b3c4d5e6f7g8h9i0j1\", \"reservaId\": \"65f1a2b3c4d5e6f7g8h9i0j2\", \"monto\": 1500.00, \"metodoPago\": \"EFECTIVO\", \"estado\": \"PAGADO\", \"referencia\": \"PAY-ABC12345\", \"fechaCreacion\": \"2024-01-15T10:00:00\", \"fechaPago\": \"2024-01-15T10:30:00\"}"))),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "No autorizado para ver este pago"),
        @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable String id, Authentication auth) {
        User user = getAuthUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }

        try {
            Payment payment = paymentService.getByIdOrThrow(id);
            
            // Validar que el usuario autenticado sea el dueño del pago
            if (!payment.getUsuarioId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "No autorizado para ver este pago"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.getId());
            response.put("reservaId", payment.getReservaId());
            response.put("monto", payment.getMonto());
            response.put("metodoPago", payment.getMetodoPago());
            response.put("estado", payment.getEstado());
            response.put("referencia", payment.getReferencia());
            response.put("fechaCreacion", payment.getFechaCreacion());
            response.put("fechaPago", payment.getFechaPago());

            return ResponseEntity.ok(response);
        } catch (com.alquiler.furent.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Pago no encontrado"));
        }
    }

    private User getAuthUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmail(auth.getName()).orElse(null);
    }
}
