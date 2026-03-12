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

    public AdminReservasController(ReservationService reservationService, AuditLogService auditLogService,
            NotificationService notificationService, PdfService pdfService) {
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pdfService = pdfService;
    }

    @GetMapping("/reservas")
    public String listReservations(Model model) {
        model.addAttribute("reservas", reservationService.getAllReservations());
        model.addAttribute("pendientes", reservationService.getPendingReservations());
        model.addAttribute("activas", reservationService.getActiveReservations());
        model.addAttribute("completadas", reservationService.getCompletedReservations());
        return "admin/reservas";
    }

    @PostMapping("/reservas/estado/{id}")
    public String updateReservationStatus(@PathVariable String id, @RequestParam String estado,
            @RequestParam(required = false) String nota,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.updateStatus(id, estado, authentication.getName(), nota);
            auditLogService.log(authentication.getName(), "CAMBIAR_ESTADO", "RESERVA", id, "Estado: " + estado);

            Reservation res = reservationService.getById(id).orElse(null);
            if (res != null) {
                notificationService.notify(res.getUsuarioId(), "Reserva Actualizada",
                        "Tu reserva ha cambiado a estado: " + estado, "INFO", "/panel");
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
