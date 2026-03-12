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
        when(reservationService.getById("res-001")).thenReturn(Optional.of(testReservation));
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
        verify(notificationService).notify(eq("user-001"), anyString(), anyString(), eq("INFO"), anyString());
    }

    @Test
    void initPayment_reservationNotFound_throwsException() {
        when(reservationService.getById("res-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.initPayment("res-999", "user-001", "NEQUI"));
    }

    @Test
    void confirmPayment_validPendingPayment_confirmsSuccessfully() {
        when(paymentRepository.findById("pay-001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        Payment result = paymentService.confirmPayment("pay-001", "REF-123", "admin@test.com");

        assertEquals("PAGADO", result.getEstado());
        assertEquals("REF-123", result.getReferencia());
        assertNotNull(result.getFechaPago());
        verify(reservationService).updateStatus("res-001", "ACTIVA");
        verify(notificationService).notify(eq("user-001"), anyString(), anyString(), eq("SUCCESS"), anyString());
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
        verify(notificationService).notify(eq("user-001"), anyString(), anyString(), eq("ALERT"), anyString());
    }
}
