package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ProductService productService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ContactService contactService;

    public AdminDashboardController(ProductService productService, ReservationService reservationService,
            UserService userService, AuditLogService auditLogService, ContactService contactService) {
        this.productService = productService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.contactService = contactService;
    }

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("totalProductos", productService.countProducts());
        model.addAttribute("totalCategorias", productService.countCategories());
        model.addAttribute("totalReservas", reservationService.count());
        model.addAttribute("reservasPendientes", reservationService.countByEstado("PENDIENTE"));
        model.addAttribute("reservasActivas", reservationService.countByEstado("ACTIVA"));
        model.addAttribute("totalUsuarios", userService.count());
        model.addAttribute("totalIngresos", reservationService.calculateTotalRevenue());
        model.addAttribute("revenueData", reservationService.getRevenueByDay());
        model.addAttribute("statusData", reservationService.getStatusDistribution());
        model.addAttribute("ultimasReservas", reservationService.getRecentActivity());
        model.addAttribute("auditLogs", auditLogService.getRecentLogs());
        model.addAttribute("mensajesNoLeidos", contactService.countUnread());
        return "admin/dashboard";
    }


}
