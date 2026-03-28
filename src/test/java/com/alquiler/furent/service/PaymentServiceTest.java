package com.alquiler.furent.service;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Payment;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private MetricsConfig metricsConfig;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testReservation = new Reservation();
        testReservation.setId("res-001");
        testReservation.setTotal(BigDecimal.valueOf(150000));
        testReservation.setUsuarioId("user-001");

        testPayment = new Payment();
        testPayment.setId("pay-001");
        testPayment.setReservaId("res-001");
        testPayment.setUsuarioId("user-001");
        testPayment.setMonto(BigDecimal.valueOf(150000));
        testPayment.setEstado("PENDIENTE");
    }

    @Test
    void initPayment_validReservation_createsPayment() {
        testReservation.setEstado("CONFIRMADA");
        when(reservationService.getByIdOrThrow("res-001")).thenReturn(testReservation);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId("pay-new");
            return p;
        });

        Payment result = paymentService.initPayment("res-001", "user-001", "NEQUI");

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(result.getMonto()));
        assertEquals("PENDIENTE", result.getEstado());
        assertEquals("NEQUI", result.getMetodoPago());
        assertNotNull(result.getReferencia());
        assertTrue(result.getReferencia().startsWith("PAY-"));
        assertEquals(12, result.getReferencia().length()); // PAY- + 8 chars
        verify(notificationService).notify(eq("user-001"), eq("Pago iniciado"), anyString(), eq("PAGO"), anyString());
    }

    @Test
    void initPayment_reservationNotFound_throwsException() {
        when(reservationService.getByIdOrThrow("res-999")).thenThrow(new ResourceNotFoundException("Reserva", "res-999"));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.initPayment("res-999", "user-001", "NEQUI"));
    }

    @Test
    void initPayment_reservationNotConfirmed_throwsException() {
        testReservation.setEstado("PENDIENTE");
        when(reservationService.getByIdOrThrow("res-001")).thenReturn(testReservation);

        InvalidOperationException exception = assertThrows(InvalidOperationException.class,
                () -> paymentService.initPayment("res-001", "user-001", "NEQUI"));
        
        assertTrue(exception.getMessage().contains("CONFIRMADA"));
        verify(paymentRepository, never()).save(any());
        verify(notificationService, never()).notify(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void generateReference_createsValidFormat() {
        String ref = paymentService.generateReference();
        
        assertNotNull(ref);
        assertTrue(ref.startsWith("PAY-"));
        assertEquals(12, ref.length()); // PAY- (4) + 8 alphanumeric
        assertTrue(ref.substring(4).matches("[A-Z0-9]{8}"));
    }

    @Test
    void confirmPayment_validPendingPayment_confirmsSuccessfully() {
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        Payment result = paymentService.confirmPayment("pay-001", "REF-123", "admin@test.com");

        assertEquals("PAGADO", result.getEstado());
        assertNotNull(result.getFechaPago());
        verify(reservationService).updateStatus(eq("res-001"), eq("ENTREGADA"), eq("admin@test.com"), anyString());
        verify(notificationService).notify(eq("user-001"), anyString(), anyString(), eq("PAGO"), anyString());
        verify(metricsConfig.getPaymentsCompleted()).increment();
        verify(auditLogService).log(eq("admin@test.com"), eq("CONFIRMAR_PAGO"), eq("PAGO"), eq("pay-001"), anyString());
    }

    @Test
    void confirmPayment_alreadyPaid_throwsException() {
        testPayment.setEstado("PAGADO");
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(testPayment));

        assertThrows(InvalidOperationException.class,
                () -> paymentService.confirmPayment("pay-001", "REF-123", "admin"));
    }

    @Test
    void failPayment_pendingPayment_marksAsFailed() {
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        Payment result = paymentService.failPayment("pay-001", "Comprobante inválido", "admin@test.com");

        assertEquals("FALLIDO", result.getEstado());
        verify(notificationService).notify(eq("user-001"), anyString(), anyString(), eq("PAGO"), anyString());
        verify(metricsConfig.getPaymentsFailed()).increment();
        verify(auditLogService).log(eq("admin@test.com"), eq("RECHAZAR_PAGO"), eq("PAGO"), eq("pay-001"), anyString());
    }

    @Test
    void getByIdOrThrow_existingPayment_returnsPayment() {
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getByIdOrThrow("pay-001");

        assertNotNull(result);
        assertEquals("pay-001", result.getId());
    }

    @Test
    void getByIdOrThrow_nonExistingPayment_throwsException() {
        when(paymentRepository.findById("pay-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getByIdOrThrow("pay-999"));
    }
}
