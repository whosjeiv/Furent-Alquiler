package com.alquiler.furent.service;

import com.alquiler.furent.model.WorkerPayment;
import com.alquiler.furent.repository.WorkerPaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkerPaymentService {

    private final WorkerPaymentRepository workerPaymentRepository;
    private final EmailService emailService;

    public WorkerPaymentService(WorkerPaymentRepository workerPaymentRepository, EmailService emailService) {
        this.workerPaymentRepository = workerPaymentRepository;
        this.emailService = emailService;
    }

    public List<WorkerPayment> getAll() {
        return workerPaymentRepository.findAll();
    }

    public Optional<WorkerPayment> getById(String id) {
        return workerPaymentRepository.findById(id);
    }

    public WorkerPayment save(WorkerPayment payment) {
        return workerPaymentRepository.save(payment);
    }

    /**
     * Sends a payment email with the PDF receipt attached.
     */
    public void sendPaymentEmailWithPdf(WorkerPayment payment, byte[] pdfBytes, String pdfFilename) {
        if (payment.getTrabajadorEmail() == null || payment.getTrabajadorEmail().isBlank()) {
            return;
        }

        String periodo = payment.getPeriodoInicio() != null && payment.getPeriodoFin() != null
                ? payment.getPeriodoInicio() + " a " + payment.getPeriodoFin()
                : "período de pago";
        String nombre = payment.getTrabajadorNombre() != null ? payment.getTrabajadorNombre() : "Colaborador";

        emailService.sendWorkerPaymentWithPdf(
                payment.getTrabajadorEmail(),
                nombre,
                periodo,
                payment.getMonto(),
                pdfBytes,
                pdfFilename
        );

        payment.setEstado("ENVIADO");
        workerPaymentRepository.save(payment);
    }

    /**
     * @deprecated Use {@link #sendPaymentEmailWithPdf(WorkerPayment, byte[], String)} instead.
     */
    @Deprecated
    public void sendPaymentEmail(WorkerPayment payment) {
        if (payment.getTrabajadorEmail() == null || payment.getTrabajadorEmail().isBlank()) {
            return;
        }
        String periodo = payment.getPeriodoInicio() != null && payment.getPeriodoFin() != null
                ? payment.getPeriodoInicio() + " a " + payment.getPeriodoFin()
                : "período de pago";
        String subjectNombre = payment.getTrabajadorNombre() != null ? payment.getTrabajadorNombre() : "colaborador";
        emailService.sendWorkerPaymentNotification(
                payment.getTrabajadorEmail(),
                subjectNombre,
                periodo,
                payment.getMonto()
        );
        payment.setEstado("ENVIADO");
        workerPaymentRepository.save(payment);
    }
}
