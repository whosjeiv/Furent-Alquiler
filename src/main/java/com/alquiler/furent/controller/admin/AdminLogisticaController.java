package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/logistica")
public class AdminLogisticaController {

    private final ReservationService reservationService;
    private final PdfService pdfService;

    public AdminLogisticaController(ReservationService reservationService, PdfService pdfService) {
        this.reservationService = reservationService;
        this.pdfService = pdfService;
    }

    @GetMapping("")
    public String logistica(Model model) {
        model.addAttribute("activeMenu", "logistica");
        List<Reservation> reservations = reservationService.getAllReservations();

        List<Map<String, Object>> eventsList = reservations.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("usuarioNombre", r.getUsuarioNombre());
            map.put("fechaInicio", r.getFechaInicio() != null ? r.getFechaInicio().toString() : null);
            map.put("fechaFin", r.getFechaFin() != null ? r.getFechaFin().toString() : null);
            map.put("estado", r.getEstado());
            map.put("direccionEvento", r.getDireccionEvento());

            if (r.getItems() != null) {
                map.put("items", r.getItems().stream().map(i -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productoNombre", i.getProductoNombre());
                    itemMap.put("cantidad", i.getCantidad());
                    return itemMap;
                }).collect(Collectors.toList()));
            } else {
                map.put("items", new ArrayList<>());
            }

            return map;
        }).collect(Collectors.toList());

        model.addAttribute("reservas", eventsList);
        return "admin/logistica";
    }

    @GetMapping("/hoja-ruta")
    public ResponseEntity<byte[]> downloadHojaRuta(@RequestParam(required = false) String fecha) {
        LocalDate date = (fecha != null && !fecha.isEmpty()) ? LocalDate.parse(fecha) : LocalDate.now();

        List<Reservation> todas = reservationService.getAllReservations();
        List<Reservation> logic = todas.stream()
                .filter(r -> (r.getFechaInicio() != null && r.getFechaInicio().equals(date)) ||
                        (r.getFechaFin() != null && r.getFechaFin().equals(date)))
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("fecha", date);
        data.put("reservas", logic);
        data.put("totalEntregas",
                logic.stream().filter(r -> r.getFechaInicio() != null && r.getFechaInicio().equals(date)).count());
        data.put("totalRecogidas",
                logic.stream().filter(r -> r.getFechaFin() != null && r.getFechaFin().equals(date)).count());

        byte[] pdf = pdfService.generatePdfFromHtml("admin/pdf/hoja-ruta", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hoja_ruta_" + date + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
