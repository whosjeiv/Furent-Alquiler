package com.alquiler.furent.controller.admin;

import com.alquiler.furent.model.WorkerPayment;
import com.alquiler.furent.service.PdfService;
import com.alquiler.furent.service.WorkerPaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/pagos-trabajadores")
public class AdminWorkerPaymentsController {

    private final WorkerPaymentService workerPaymentService;
    private final PdfService pdfService;

    public AdminWorkerPaymentsController(WorkerPaymentService workerPaymentService, PdfService pdfService) {
        this.workerPaymentService = workerPaymentService;
        this.pdfService = pdfService;
    }

    @GetMapping("")
    public String list(Model model) {
        List<WorkerPayment> pagos = workerPaymentService.getAll();
        model.addAttribute("pagos", pagos);
        model.addAttribute("activeMenu", "pagos-trabajadores");
        return "admin/pagos-trabajadores";
    }

    @PostMapping("")
    public String create(
            @RequestParam String trabajadorNombre,
            @RequestParam String trabajadorEmail,
            @RequestParam(required = false) String trabajadorRol,
            @RequestParam String periodoTipo,
            @RequestParam String periodoInicio,
            @RequestParam String periodoFin,
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) BigDecimal tarifaHora,
            @RequestParam(required = false) Integer horasTrabajadas,
            @RequestParam(required = false) Integer eventosAtendidos,
            @RequestParam(required = false) String notas,
            Model model
    ) {
        WorkerPayment payment = new WorkerPayment();
        payment.setTrabajadorNombre(trabajadorNombre);
        payment.setTrabajadorEmail(trabajadorEmail);
        payment.setTrabajadorRol(trabajadorRol);
        payment.setPeriodoTipo(periodoTipo);
        payment.setPeriodoInicio(LocalDate.parse(periodoInicio));
        payment.setPeriodoFin(LocalDate.parse(periodoFin));
        payment.setMonto(monto);
        payment.setTarifaHora(tarifaHora);
        payment.setHorasTrabajadas(horasTrabajadas);
        payment.setEventosAtendidos(eventosAtendidos);
        payment.setNotas(notas);

        workerPaymentService.save(payment);

        model.addAttribute("success", "Pago generado correctamente para " + trabajadorNombre);
        return "redirect:/admin/pagos-trabajadores";
    }

    /**
     * Builds the data map for the PDF template, pre-formatting dates and numbers
     * to avoid relying on Thymeleaf utility objects that are incompatible with OpenHTMLtoPDF.
     */
    private Map<String, Object> buildPdfData(WorkerPayment payment) {
        Map<String, Object> data = new HashMap<>();
        data.put("pago", payment);

        // Pre-format dates as strings
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        data.put("periodoInicioStr",
                payment.getPeriodoInicio() != null ? payment.getPeriodoInicio().format(dateFormatter) : "—");
        data.put("periodoFinStr",
                payment.getPeriodoFin() != null ? payment.getPeriodoFin().format(dateFormatter) : "—");
        data.put("fechaCreacionStr",
                payment.getFechaCreacion() != null ? payment.getFechaCreacion().format(dateTimeFormatter) : "—");

        // Pre-format numbers as strings
        data.put("montoStr",
                payment.getMonto() != null ? "$" + String.format("%,.0f", payment.getMonto()) : "$0");
        data.put("tarifaHoraStr",
                payment.getTarifaHora() != null ? "$" + String.format("%,.0f", payment.getTarifaHora()) : "$0");

        return data;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String id) {
        WorkerPayment payment = workerPaymentService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago de trabajador no encontrado"));

        Map<String, Object> data = buildPdfData(payment);
        byte[] pdf = pdfService.generatePdfFromHtml("admin/pdf/pago-trabajador", data);

        String filename = "pago_trabajador_" + payment.getTrabajadorNombre() + "_" + payment.getId() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename.replace(" ", "_"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/enviar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enviar(@PathVariable String id) {
        WorkerPayment payment = workerPaymentService.getById(id).orElse(null);
        if (payment == null) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Pago no encontrado");
            return ResponseEntity.badRequest().body(errorBody);
        }

        // Generate the PDF to attach to the email
        Map<String, Object> pdfData = buildPdfData(payment);
        byte[] pdfBytes = pdfService.generatePdfFromHtml("admin/pdf/pago-trabajador", pdfData);
        String pdfFilename = "Comprobante_Pago_" + (payment.getTrabajadorNombre() != null
                ? payment.getTrabajadorNombre().replace(" ", "_") : "trabajador") + ".pdf";

        workerPaymentService.sendPaymentEmailWithPdf(payment, pdfBytes, pdfFilename);

        Map<String, Object> okBody = new HashMap<>();
        okBody.put("success", true);
        okBody.put("message", "Correo enviado a " + payment.getTrabajadorEmail());
        return ResponseEntity.ok(okBody);
    }
}

