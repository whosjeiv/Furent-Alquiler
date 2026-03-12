package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminPagosController {

    private final PaymentService paymentService;

    public AdminPagosController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/pagos")
    @ResponseBody
    public ResponseEntity<List<Payment>> listPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/pagos/confirmar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmPayment(@PathVariable String id,
            @RequestParam(required = false) String referencia,
            Authentication auth) {
        try {
            paymentService.confirmPayment(id, referencia != null ? referencia : "", auth.getName());
            return ResponseEntity.ok(Map.of("success", true, "message", "Pago confirmado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
