package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminReservasController {

    private final ReservationService reservationService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PdfService pdfService;
    private final UserService userService;
    private final EmailService emailService;

    public AdminReservasController(ReservationService reservationService, AuditLogService auditLogService,
            NotificationService notificationService, PdfService pdfService,
            UserService userService, EmailService emailService) {
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pdfService = pdfService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/reservas")
    public String listReservations(Model model) {
        model.addAttribute("reservas", reservationService.getAllReservations());
        model.addAttribute("pendientes", reservationService.getPendingReservations());
        model.addAttribute("entregadas", reservationService.getActiveReservations());
        model.addAttribute("completadas", reservationService.getCompletedReservations());
        return "admin/reservas";
    }

    @PostMapping("/reservas/eliminar/{id}")
    public String deleteReservation(@PathVariable String id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.deleteById(id);
            if (authentication != null) {
                auditLogService.log(authentication.getName(), "ELIMINAR_COTIZACION", "RESERVA", id,
                        "Reserva/cotización eliminada manualmente desde el panel admin");
            }
            redirectAttributes.addFlashAttribute("success", "Cotización eliminada correctamente.");
        } catch (com.alquiler.furent.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", "La cotización ya no existe.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar la cotización. Intenta de nuevo.");
        }
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/estado/{id}")
    public String updateReservationStatus(@PathVariable String id, @RequestParam String estado,
            @RequestParam(required = false) String nota,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // FIX: Validar que el estado sea un valor válido del enum
            try {
                com.alquiler.furent.enums.EstadoReserva.valueOf(estado);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Estado no válido: " + estado);
                return "redirect:/admin/reservas";
            }

            reservationService.updateStatus(id, estado, authentication.getName(), nota);
            auditLogService.log(authentication.getName(), "CAMBIAR_ESTADO", "RESERVA", id, "Estado: " + estado);

            Reservation res = reservationService.getById(id).orElse(null);
            if (res != null) {
                notificationService.notify(res.getUsuarioId(), "Reserva Actualizada",
                        "Tu reserva ha cambiado a estado: " + estado, "INFO", "/panel");

                userService.findById(res.getUsuarioId()).ifPresent(user -> {
                    if (user.isNotificacionesEmail()) {
                        emailService.sendStatusChange(user.getEmail(), res.getId(), estado);
                    }
                });
            }
            redirectAttributes.addFlashAttribute("success", "Estado de reserva actualizado a " + estado);
        } catch (com.alquiler.furent.exception.InvalidOperationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @GetMapping("/reservas/contrato/{id}")
    public ResponseEntity<byte[]> downloadContrato(@PathVariable String id) {
        Reservation reservation = reservationService.getById(id).orElseThrow();

        Map<String, Object> data = new HashMap<>();
        data.put("r", reservation);
        data.put("fechaContrato", LocalDate.now());

        byte[] pdf = pdfService.generatePdfFromHtml("admin/pdf/contrato", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contrato_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


}
