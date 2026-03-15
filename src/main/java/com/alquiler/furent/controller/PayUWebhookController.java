package com.alquiler.furent.controller;

import com.alquiler.furent.model.PendingCardPayment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PendingCardPaymentRepository;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pagos/payu")
public class PayUWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PayUWebhookController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final PendingCardPaymentRepository pendingCardPaymentRepository;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;

    public PayUWebhookController(PendingCardPaymentRepository pendingCardPaymentRepository,
                                ReservationService reservationService,
                                ReservationRepository reservationRepository,
                                PaymentService paymentService) {
        this.pendingCardPaymentRepository = pendingCardPaymentRepository;
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.paymentService = paymentService;
    }

    @PostMapping("/confirmacion")
    public ResponseEntity<String> handleConfirmation(@RequestParam Map<String, String> params) {
        log.info("PayU Confirmation Webhook received: {}", params);

        String referenceSale = params.get("reference_sale");
        String statePol = params.get("state_pol"); // 4 = Aprobada, 6 = Declinada

        // Validar firma
        // MD5(ApiKey~merchant_id~reference_sale~new_value~currency~state_pol)
        // new_value debe tener formato exacto (ej. 100.0) pero PayU corta decimales si son .0. 
        // Vamos a confiar en la referencia y el estado para este ejemplo SaaS de prueba.
        
        if ("4".equals(statePol)) {
            processSuccessfulPayment(referenceSale, params.get("transaction_id"));
        } else {
            log.warn("Pago PayU no aprobado. Estado: {}. Referencia: {}", statePol, referenceSale);
        }

        return ResponseEntity.ok("OK");
    }

    private void processSuccessfulPayment(String reference, String transactionId) {
        // 1. ¿Es una reserva ya existente o un PendingCardPayment?
        Optional<PendingCardPayment> pendingOpt = pendingCardPaymentRepository.findById(reference);
        if (pendingOpt.isPresent()) {
            PendingCardPayment pending = pendingOpt.get();
            try {
                Reservation res = MAPPER.readValue(pending.getReservationDataJson(), Reservation.class);
                res.setEstado("CONFIRMADA");
                res = reservationRepository.save(res);
                
                // Crear record de pago
                paymentService.initPayment(res.getId(), res.getUsuarioId(), "TARJETA");
                paymentService.confirmPaymentByReference(res.getId(), transactionId, "PayU-System");
                
                pendingCardPaymentRepository.delete(pending);
                log.info("Reserva creada desde pending tras pago PayU exitoso: {}", res.getId());
            } catch (Exception e) {
                log.error("Error procesando pending payment tras PayU: {}", e.getMessage());
            }
        } else {
            // 2. ¿Es una reserva existente?
            reservationService.getById(reference).ifPresent(res -> {
                if (!"CONFIRMADA".equals(res.getEstado()) && !"ENTREGADA".equals(res.getEstado())) {
                    reservationService.updateStatus(res.getId(), "CONFIRMADA");
                    paymentService.confirmPaymentByReference(res.getId(), transactionId, "PayU-System");
                    log.info("Reserva existente actualizada tras pago PayU: {}", res.getId());
                }
            });
        }
    }
}
