package com.alquiler.furent.controller;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.PaymentService;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final ReservationService reservationService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, ReservationService reservationService,
                             UserService userService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        this.userService = userService;
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
        Payment payment = paymentService.initPayment(reservaId, user.getId(), metodoPago);

        log.info("Pago iniciado por usuario {} para reserva {}", user.getEmail(), reservaId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", payment.getId(),
                "monto", payment.getMonto(),
                "metodoPago", payment.getMetodoPago()
        ));
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
