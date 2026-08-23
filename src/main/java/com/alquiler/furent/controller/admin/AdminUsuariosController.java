package com.alquiler.furent.controller.admin;

import com.alquiler.furent.dto.UserAdminDetailsDTO;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.AuditLogService;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminUsuariosController {

    private final UserService userService;
    private final ReservationService reservationService;
    private final AuditLogService auditLogService;

    public AdminUsuariosController(UserService userService, ReservationService reservationService, AuditLogService auditLogService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/usuarios")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size,
                            Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(10, Math.min(size, 100));

        Page<User> usersPage = userService.getUsersPage(safePage, safeSize);
        model.addAttribute("usuarios", usersPage.getContent());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalUsuarios", usersPage.getTotalElements());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/rol/{id}")
    @ResponseBody
    public String updateRole(@PathVariable String id, @RequestParam String role) {
        com.alquiler.furent.model.User user = userService.findById(id).orElseThrow();
        user.setRole(role);
        userService.save(user);
        return "OK";
    }

    @PostMapping("/usuarios/estado/{id}")
    @ResponseBody
    public String updateEstado(@PathVariable String id, @RequestParam boolean activo,
            @RequestParam(required = false) String razon,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "false") boolean permanente,
            Authentication authentication) {

        User user = userService.findById(id).orElseThrow();
        user.setActivo(activo);

        if (!activo) {
            user.setRazonSuspension(razon != null && !razon.isEmpty() ? razon : "Violación de términos del sistema");
            user.setSuspensionPermanente(permanente);

            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                user.setFechaInicioSuspension(LocalDateTime.parse(fechaInicio + "T00:00:00"));
            } else {
                user.setFechaInicioSuspension(LocalDateTime.now());
            }

            if (!permanente && fechaFin != null && !fechaFin.isEmpty()) {
                user.setFechaFinSuspension(LocalDateTime.parse(fechaFin + "T23:59:59"));
            } else if (!permanente) {
                user.setFechaFinSuspension(user.getFechaInicioSuspension().plusDays(7));
            } else {
                user.setFechaFinSuspension(null);
            }
        } else {
            user.setRazonSuspension(null);
            user.setFechaInicioSuspension(null);
            user.setFechaFinSuspension(null);
            user.setSuspensionPermanente(false);
        }

        userService.save(user);
        String action = activo ? "ACTIVAR_USUARIO" : "SUSPENDER_USUARIO";
        String detail = activo ? "Usuario activado: " + user.getEmail()
                : "Usuario suspendido: " + user.getEmail() + " | Razón: " + user.getRazonSuspension();
        auditLogService.log(authentication.getName(), action, "USUARIO", id, detail);
        return "OK";
    }

    @GetMapping("/usuarios/detalles/{id}")
    @ResponseBody
    public UserAdminDetailsDTO getDetalles(@PathVariable String id) {
        User user = userService.findById(id).orElseThrow();
        java.util.List<Reservation> userReservas = reservationService.getByUsuarioId(id);
        
        long totalReservas = userReservas.size();
        java.math.BigDecimal totalInvertido = userReservas.stream()
                .filter(r -> "COMPLETADA".equals(r.getEstado()) || "CONFIRMADA".equals(r.getEstado()) || "ENTREGADA".equals(r.getEstado()))
                .map(Reservation::getTotal)
                .filter(t -> t != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        long favoritosCount = user.getFavoritos() != null ? user.getFavoritos().size() : 0;
        
        return new UserAdminDetailsDTO(user, totalReservas, totalInvertido, favoritosCount);
    }
}
