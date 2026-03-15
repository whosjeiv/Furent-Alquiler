package com.alquiler.furent.controller;

import com.alquiler.furent.service.StripeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recibe webhooks de Stripe (p. ej. payment_intent.succeeded).
 * La firma se verifica en el servicio; este endpoint debe ser público (sin auth).
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeService stripeService;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {
        if (stripeSignature == null || stripeSignature.isBlank()) {
            log.warn("Webhook Stripe recibido sin cabecera Stripe-Signature");
            return ResponseEntity.badRequest().body("Missing Stripe-Signature");
        }
        boolean handled = stripeService.handleWebhookEvent(payload, stripeSignature);
        return handled ? ResponseEntity.ok("OK") : ResponseEntity.status(400).body("Webhook error");
    }
}
