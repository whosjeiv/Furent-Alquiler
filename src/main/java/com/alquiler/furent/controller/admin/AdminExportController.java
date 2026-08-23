package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.ExportService;
import com.alquiler.furent.service.ProductService;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminExportController {

    private final ExportService exportService;
    private final ProductService productService;
    private final ReservationService reservationService;
    private final UserService userService;

    public AdminExportController(ExportService exportService, ProductService productService,
            ReservationService reservationService, UserService userService) {
        this.exportService = exportService;
        this.productService = productService;
        this.reservationService = reservationService;
        this.userService = userService;
    }

    @GetMapping("/exportar/productos")
    public ResponseEntity<byte[]> exportProducts() {
        byte[] csv = exportService.exportProductsCsv(productService.getAllProducts());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/exportar/reservas")
    public ResponseEntity<byte[]> exportReservations() {
        byte[] csv = exportService.exportReservationsCsv(reservationService.getAllReservations());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reservas.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/exportar/usuarios")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] csv = exportService.exportUsersCsv(userService.getAllUsers());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usuarios.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
