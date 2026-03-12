package com.alquiler.furent.service;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private MetricsConfig metricsConfig;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testReservation = new Reservation();
        testReservation.setId("res-001");
        testReservation.setEstado("PENDIENTE");
        testReservation.setUsuarioId("user-001");
        testReservation.setUsuarioNombre("Test User");
    }

    // ============================================================
    // STATE MACHINE — VALID FORWARD TRANSITIONS
    // ============================================================

    @Nested
    @DisplayName("Transiciones de estado válidas (avance)")
    class ValidForwardTransitions {

        @Test
        void pendiente_to_confirmada() {
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "CONFIRMADA", "admin@test.com", "Aprobado");

            assertEquals("CONFIRMADA", testReservation.getEstado());
            verify(statusHistoryRepository).save(any());
            verify(reservationRepository).save(testReservation);
        }

        @Test
        void confirmada_to_activa() {
            testReservation.setEstado("CONFIRMADA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "ACTIVA", "admin", null);

            assertEquals("ACTIVA", testReservation.getEstado());
        }

        @Test
        void activa_to_enCurso() {
            testReservation.setEstado("ACTIVA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "EN_CURSO", "admin", "Entrega en camino");

            assertEquals("EN_CURSO", testReservation.getEstado());
        }

        @Test
        void enCurso_to_completada() {
            testReservation.setEstado("EN_CURSO");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "COMPLETADA", "admin", "Entrega completada");

            assertEquals("COMPLETADA", testReservation.getEstado());
        }

        @Test
        void activa_to_completada() {
            testReservation.setEstado("ACTIVA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "COMPLETADA", "admin", "Completada directamente");

            assertEquals("COMPLETADA", testReservation.getEstado());
        }
    }

    // ============================================================
    // STATE MACHINE — CANCELACIÓN DESDE CUALQUIER ESTADO
    // ============================================================

    @Nested
    @DisplayName("Cancelación desde cualquier estado activo")
    class CancellationFromAnyState {

        @Test
        void cancelar_desde_pendiente() {
            testReservation.setEstado("PENDIENTE");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "CANCELADA", "admin@test.com", null);

            assertEquals("CANCELADA", testReservation.getEstado());
            verify(eventPublisher).publish(any());
        }

        @Test
        void cancelar_desde_confirmada() {
            testReservation.setEstado("CONFIRMADA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "CANCELADA", "admin", "Cliente desistió");

            assertEquals("CANCELADA", testReservation.getEstado());
        }

        @Test
        void cancelar_desde_activa() {
            testReservation.setEstado("ACTIVA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "CANCELADA", "admin", null);

            assertEquals("CANCELADA", testReservation.getEstado());
        }

        @Test
        void cancelar_desde_enCurso() {
            testReservation.setEstado("EN_CURSO");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            reservationService.updateStatus("res-001", "CANCELADA", "admin", "Emergencia");

            assertEquals("CANCELADA", testReservation.getEstado());
        }
    }

    // ============================================================
    // STATE MACHINE — TRANSICIONES INVÁLIDAS
    // ============================================================

    @Nested
    @DisplayName("Transiciones de estado inválidas")
    class InvalidTransitions {

        @ParameterizedTest(name = "PENDIENTE → {0} debe fallar")
        @CsvSource({"ACTIVA", "EN_CURSO", "COMPLETADA"})
        void pendiente_invalid_transitions(String target) {
            testReservation.setEstado("PENDIENTE");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", target, "admin", null));
        }

        @ParameterizedTest(name = "CONFIRMADA → {0} debe fallar")
        @CsvSource({"PENDIENTE", "EN_CURSO", "COMPLETADA"})
        void confirmada_invalid_transitions(String target) {
            testReservation.setEstado("CONFIRMADA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", target, "admin", null));
        }

        @ParameterizedTest(name = "ACTIVA → {0} debe fallar")
        @CsvSource({"PENDIENTE", "CONFIRMADA"})
        void activa_invalid_transitions(String target) {
            testReservation.setEstado("ACTIVA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", target, "admin", null));
        }

        @ParameterizedTest(name = "EN_CURSO → {0} debe fallar")
        @CsvSource({"PENDIENTE", "CONFIRMADA", "ACTIVA"})
        void enCurso_invalid_transitions(String target) {
            testReservation.setEstado("EN_CURSO");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", target, "admin", null));
        }

        @Test
        @DisplayName("COMPLETADA es terminal — no permite transiciones")
        void completada_is_terminal() {
            testReservation.setEstado("COMPLETADA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", "PENDIENTE", "admin", null));
            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", "ACTIVA", "admin", null));
        }

        @Test
        @DisplayName("CANCELADA es terminal — no permite transiciones")
        void cancelada_is_terminal() {
            testReservation.setEstado("CANCELADA");
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", "PENDIENTE", "admin", null));
            assertThrows(InvalidOperationException.class,
                    () -> reservationService.updateStatus("res-001", "CONFIRMADA", "admin", null));
        }

        @Test
        @DisplayName("Reserva no encontrada lanza ResourceNotFoundException")
        void notFound_throwsException() {
            when(reservationRepository.findById("res-999")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservationService.updateStatus("res-999", "CONFIRMADA", "admin", null));
        }
    }

    // ============================================================
    // VALIDACIÓN DE FECHAS
    // ============================================================

    @Nested
    @DisplayName("Validación de fechas en save()")
    class DateValidation {

        @Test
        @DisplayName("fechaInicio en el pasado lanza excepción")
        void save_rejects_past_startDate() {
            testReservation.setFechaInicio(LocalDate.now().minusDays(1));
            testReservation.setFechaFin(LocalDate.now().plusDays(3));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.save(testReservation));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("fechaFin en el pasado lanza excepción")
        void save_rejects_past_endDate() {
            testReservation.setFechaInicio(LocalDate.now());
            testReservation.setFechaFin(LocalDate.now().minusDays(1));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.save(testReservation));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("fechaFin antes de fechaInicio lanza excepción")
        void save_rejects_endDate_before_startDate() {
            testReservation.setFechaInicio(LocalDate.now().plusDays(5));
            testReservation.setFechaFin(LocalDate.now().plusDays(2));

            assertThrows(InvalidOperationException.class,
                    () -> reservationService.save(testReservation));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fechas válidas permite guardar")
        void save_accepts_valid_dates() {
            testReservation.setFechaInicio(LocalDate.now());
            testReservation.setFechaFin(LocalDate.now().plusDays(3));
            when(reservationRepository.save(any())).thenReturn(testReservation);

            Reservation result = reservationService.save(testReservation);

            assertNotNull(result);
            verify(reservationRepository).save(testReservation);
        }

        @Test
        @DisplayName("Fechas nulas permite guardar (sin validación)")
        void save_accepts_null_dates() {
            testReservation.setFechaInicio(null);
            testReservation.setFechaFin(null);
            when(reservationRepository.save(any())).thenReturn(testReservation);

            Reservation result = reservationService.save(testReservation);

            assertNotNull(result);
            verify(reservationRepository).save(testReservation);
        }
    }

    // ============================================================
    // CÁLCULOS DE INGRESOS
    // ============================================================

    @Nested
    @DisplayName("Cálculo de ingresos y estadísticas")
    class RevenueCalculations {

        @Test
        @DisplayName("calculateTotalRevenue incluye solo estados COMPLETADA, ACTIVA, CONFIRMADA")
        void calculateTotalRevenue_filtersCorrectStates() {
            Reservation completada = createReservation("r1", "COMPLETADA", new BigDecimal("100000"));
            Reservation activa = createReservation("r2", "ACTIVA", new BigDecimal("50000"));
            Reservation confirmada = createReservation("r3", "CONFIRMADA", new BigDecimal("75000"));
            Reservation pendiente = createReservation("r4", "PENDIENTE", new BigDecimal("200000"));
            Reservation cancelada = createReservation("r5", "CANCELADA", new BigDecimal("300000"));

            when(reservationRepository.findAll()).thenReturn(
                    List.of(completada, activa, confirmada, pendiente, cancelada));

            BigDecimal total = reservationService.calculateTotalRevenue();

            assertEquals(0, new BigDecimal("225000").compareTo(total));
        }

        @Test
        @DisplayName("calculateTotalRevenue con lista vacía retorna ZERO")
        void calculateTotalRevenue_emptyList_returnsZero() {
            when(reservationRepository.findAll()).thenReturn(List.of());

            BigDecimal total = reservationService.calculateTotalRevenue();

            assertEquals(0, BigDecimal.ZERO.compareTo(total));
        }

        @Test
        @DisplayName("getStatusDistribution agrupa correctamente")
        void getStatusDistribution_groupsByState() {
            Reservation r1 = createReservation("r1", "PENDIENTE", BigDecimal.ZERO);
            Reservation r2 = createReservation("r2", "PENDIENTE", BigDecimal.ZERO);
            Reservation r3 = createReservation("r3", "ACTIVA", BigDecimal.ZERO);

            when(reservationRepository.findAll()).thenReturn(List.of(r1, r2, r3));

            Map<String, Long> dist = reservationService.getStatusDistribution();

            assertEquals(2L, dist.get("PENDIENTE"));
            assertEquals(1L, dist.get("ACTIVA"));
        }

        @Test
        @DisplayName("getRevenueByDay agrupa ingresos por día")
        void getRevenueByDay_groupsByDay() {
            Reservation r1 = createReservation("r1", "COMPLETADA", new BigDecimal("100000"));
            r1.setFechaCreacion(LocalDateTime.of(2025, 1, 15, 10, 0));
            Reservation r2 = createReservation("r2", "ACTIVA", new BigDecimal("50000"));
            r2.setFechaCreacion(LocalDateTime.of(2025, 1, 15, 14, 0));
            Reservation r3 = createReservation("r3", "CONFIRMADA", new BigDecimal("75000"));
            r3.setFechaCreacion(LocalDateTime.of(2025, 1, 16, 9, 0));

            when(reservationRepository.findAll()).thenReturn(List.of(r1, r2, r3));

            Map<String, BigDecimal> revenue = reservationService.getRevenueByDay();

            // Should have exactly 2 different days
            assertEquals(2, revenue.size());
            // Sum of r1+r2 on day 15 = 150000
            BigDecimal day15Total = revenue.values().stream()
                    .filter(v -> v.compareTo(new BigDecimal("150000")) == 0)
                    .findFirst().orElse(null);
            assertNotNull(day15Total);
            // r3 on day 16 = 75000
            BigDecimal day16Total = revenue.values().stream()
                    .filter(v -> v.compareTo(new BigDecimal("75000")) == 0)
                    .findFirst().orElse(null);
            assertNotNull(day16Total);
        }
    }

    // ============================================================
    // CONSULTAS
    // ============================================================

    @Nested
    @DisplayName("Consultas y búsquedas")
    class Queries {

        @Test
        @DisplayName("getById retorna Optional con reserva existente")
        void getById_found() {
            when(reservationRepository.findById("res-001")).thenReturn(Optional.of(testReservation));

            Optional<Reservation> result = reservationService.getById("res-001");

            assertTrue(result.isPresent());
            assertEquals("res-001", result.get().getId());
        }

        @Test
        @DisplayName("getById retorna empty para reserva inexistente")
        void getById_notFound() {
            when(reservationRepository.findById("res-999")).thenReturn(Optional.empty());

            Optional<Reservation> result = reservationService.getById("res-999");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getByIdOrThrow lanza excepción si no existe")
        void getByIdOrThrow_notFound() {
            when(reservationRepository.findById("res-999")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservationService.getByIdOrThrow("res-999"));
        }

        @Test
        @DisplayName("count retorna total de reservas")
        void count_returnsTotal() {
            when(reservationRepository.count()).thenReturn(42L);

            assertEquals(42L, reservationService.count());
        }

        @Test
        @DisplayName("countByEstado filtra por estado")
        void countByEstado_filtersState() {
            when(reservationRepository.countByEstado("PENDIENTE")).thenReturn(5L);

            assertEquals(5L, reservationService.countByEstado("PENDIENTE"));
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Reservation createReservation(String id, String estado, BigDecimal total) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setEstado(estado);
        r.setTotal(total);
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }
}
