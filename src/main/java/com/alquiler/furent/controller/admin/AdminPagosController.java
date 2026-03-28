package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Payment;
import com.alquiler.furent.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/pagos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Pagos", description = "Gestión administrativa de pagos (solo administradores)")
public class AdminPagosController {

    private final PaymentService paymentService;

    public AdminPagosController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Listar pagos", 
               description = "Lista todos los pagos del sistema con paginación (solo administradores)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pagos obtenida exitosamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @GetMapping
    public String listPayments(@PageableDefault(size = 20) Pageable pageable, Model model) {
        Page<Payment> pagosPage = paymentService.getAllPaymentsPaged(pageable);
        model.addAttribute("pagos", pagosPage);
        return "admin/pagos";
    }

    @Operation(summary = "Confirmar pago", 
               description = "Confirma un pago pendiente, actualiza su estado a PAGADO y cambia la reserva asociada a ENTREGADA. Envía notificación por email al usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Pago confirmado exitosamente, redirige a lista de pagos"),
        @ApiResponse(responseCode = "400", description = "Error al confirmar el pago"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @PostMapping("/{id}/confirmar")
    public String confirmPayment(@PathVariable String id,
                                  @RequestParam(required = false) String referencia,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            String admin = principal.getName();
            paymentService.confirmPayment(id, referencia, admin);
            redirectAttributes.addFlashAttribute("success", "Pago confirmado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al confirmar pago: " + e.getMessage());
        }
        return "redirect:/admin/pagos";
    }

    @Operation(summary = "Rechazar pago", 
               description = "Rechaza un pago pendiente, actualiza su estado a FALLIDO y registra la razón del rechazo. Notifica al usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Pago rechazado exitosamente, redirige a lista de pagos"),
        @ApiResponse(responseCode = "400", description = "Error al rechazar el pago"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - requiere rol ADMIN")
    })
    @PostMapping("/{id}/rechazar")
    public String rejectPayment(@PathVariable String id,
                                 @RequestParam(required = false) String razon,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            String admin = principal.getName();
            paymentService.failPayment(id, razon, admin);
            redirectAttributes.addFlashAttribute("success", "Pago rechazado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al rechazar pago: " + e.getMessage());
        }
        return "redirect:/admin/pagos";
    }
}
