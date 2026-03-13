package com.alquiler.furent.service;

import com.alquiler.furent.enums.EstadoReserva;
import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.model.StatusHistory;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.StatusHistoryRepository;
import com.alquiler.furent.exception.ResourceNotFoundException;
import com.alquiler.furent.exception.InvalidOperationException;
import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.ReservationCreatedEvent;
import com.alquiler.furent.event.ReservationCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

/**
 * Servicio de gestión de reservas y cotizaciones.
 * Controla el ciclo de vida completo: creación, transiciones de estado validadas,
 * cálculo de ingresos y generación de reportes de actividad.
 *
 * @author Furent Team
 * @since 1.0
 */
@Service
public class ReservationService {

        private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

        // Transiciones de estado válidas usando constantes de EstadoReserva
        private static final Map<String, Set<String>> TRANSITIONS = Map.of(
                        EstadoReserva.PENDIENTE.name(),
                        Set.of(EstadoReserva.CONFIRMADA.name(), EstadoReserva.CANCELADA.name()),
                        EstadoReserva.CONFIRMADA.name(), Set.of(EstadoReserva.ACTIVA.name(), EstadoReserva.CANCELADA.name()),
                        EstadoReserva.ACTIVA.name(),
                        Set.of(EstadoReserva.EN_CURSO.name(), EstadoReserva.COMPLETADA.name(), EstadoReserva.CANCELADA.name()),
                        EstadoReserva.EN_CURSO.name(), Set.of(EstadoReserva.COMPLETADA.name(), EstadoReserva.CANCELADA.name()),
                        EstadoReserva.COMPLETADA.name(), Set.of(),
                        EstadoReserva.CANCELADA.name(), Set.of());

        private final ReservationRepository reservationRepository;
        private final ProductRepository productRepository;
        private final StatusHistoryRepository statusHistoryRepository;
        private final EventPublisher eventPublisher;
        private final MetricsConfig metricsConfig;

        public ReservationService(ReservationRepository reservationRepository,
                        ProductRepository productRepository,
                        StatusHistoryRepository statusHistoryRepository,
                        EventPublisher eventPublisher,
                        MetricsConfig metricsConfig) {
                this.reservationRepository = reservationRepository;
                this.productRepository = productRepository;
                this.statusHistoryRepository = statusHistoryRepository;
                this.eventPublisher = eventPublisher;
                this.metricsConfig = metricsConfig;
        }

        public List<Reservation> getAllReservations() {
                return reservationRepository.findAll();
        }

        public Optional<Reservation> getById(String id) {
                return reservationRepository.findById(id);
        }

        public Reservation getByIdOrThrow(String id) {
                return reservationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
        }

        public List<Reservation> getByUsuarioId(String usuarioId) {
                return reservationRepository.findByUsuarioId(usuarioId);
        }

        public List<Reservation> getActiveReservations() {
                return reservationRepository.findByEstado(EstadoReserva.ACTIVA.name());
        }

        public List<Reservation> getPendingReservations() {
                return reservationRepository.findByEstado(EstadoReserva.PENDIENTE.name());
        }

        public List<Reservation> getCompletedReservations() {
                return reservationRepository.findByEstado(EstadoReserva.COMPLETADA.name());
        }

        public List<Reservation> getConfirmedReservations() {
                return reservationRepository.findByEstado(EstadoReserva.CONFIRMADA.name());
        }

        public List<Reservation> getActiveReservationsByProductId(String productoId) {
                return reservationRepository.findActiveByProductoId(productoId);
        }

        public List<Reservation> getActiveReservationsByProductIds(List<String> productIds) {
                return reservationRepository.findActiveByProductIds(productIds);
        }

        public Reservation save(Reservation reservation) {
                validateDates(reservation);
                long start = System.nanoTime();
                Reservation saved = reservationRepository.save(reservation);
                metricsConfig.getReservationProcessingTime()
                                .record(java.time.Duration.ofNanos(System.nanoTime() - start));
                metricsConfig.getReservationsCreated().increment();
                String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
                eventPublisher.publish(new ReservationCreatedEvent(this, saved, tenantId));
                return saved;
        }

        private void validateDates(Reservation reservation) {
                LocalDate inicio = reservation.getFechaInicio();
                LocalDate fin = reservation.getFechaFin();
                if (inicio != null && inicio.isBefore(LocalDate.now())) {
                        throw new InvalidOperationException("La fecha de inicio no puede ser en el pasado");
                }
                if (fin != null && fin.isBefore(LocalDate.now())) {
                        throw new InvalidOperationException("La fecha de fin no puede ser en el pasado");
                }
                if (inicio != null && fin != null) {
                        if (fin.isBefore(inicio)) {
                                throw new InvalidOperationException("La fecha de fin no puede ser anterior a la fecha de inicio");
                        }
                        long days = ChronoUnit.DAYS.between(inicio, fin) + 1;
                        if (days > 7) {
                                throw new InvalidOperationException("El mobiliario puede reservarse por un máximo de 7 días");
                        }
                }
        }

        /**
         * Validates that enough stock is available for each product in the reservation
         * across the requested date range. Returns a map of productId -> error message
         * for any product that would be overbooked. Empty map = all OK.
         */
        public java.util.Map<String, String> validateAvailability(Reservation reservation) {
                java.util.Map<String, String> errors = new java.util.LinkedHashMap<>();
                if (reservation.getItems() == null || reservation.getItems().isEmpty()) return errors;
                if (reservation.getFechaInicio() == null || reservation.getFechaFin() == null) return errors;

                LocalDate reqStart = reservation.getFechaInicio();
                LocalDate reqEnd = reservation.getFechaFin();

                // Collect all product IDs requested
                List<String> productIds = reservation.getItems().stream()
                        .map(Reservation.ItemReserva::getProductoId)
                        .collect(Collectors.toList());

                // Get all overlapping active reservations (PENDIENTE, CONFIRMADA, ACTIVA)
                List<Reservation> overlapping = reservationRepository.findActiveByProductIds(productIds)
                        .stream()
                        .filter(r -> r.getFechaInicio() != null && r.getFechaFin() != null)
                        .filter(r -> !r.getFechaFin().isBefore(reqStart) && !r.getFechaInicio().isAfter(reqEnd))
                        .collect(Collectors.toList());

                // For each product, calculate peak usage across all days in the range
                for (Reservation.ItemReserva item : reservation.getItems()) {
                        String pid = item.getProductoId();
                        int requested = item.getCantidad();

                        // Get total stock from DB
                        Optional<Product> productOpt = productRepository.findById(pid);
                        if (productOpt.isEmpty()) {
                                errors.put(pid, "El producto '" + item.getProductoNombre() + "' ya no existe en el catálogo.");
                                continue;
                        }
                        int totalStock = productOpt.get().getStock();

                        // Find the day with maximum reserved units for this product
                        int maxReserved = 0;
                        LocalDate day = reqStart;
                        while (!day.isAfter(reqEnd)) {
                                final LocalDate currentDay = day;
                                int reservedThisDay = overlapping.stream()
                                        .filter(r -> !currentDay.isBefore(r.getFechaInicio()) && !currentDay.isAfter(r.getFechaFin()))
                                        .flatMap(r -> r.getItems().stream())
                                        .filter(i -> pid.equals(i.getProductoId()))
                                        .mapToInt(Reservation.ItemReserva::getCantidad)
                                        .sum();
                                if (reservedThisDay > maxReserved) maxReserved = reservedThisDay;
                                day = day.plusDays(1);
                        }

                        int available = totalStock - maxReserved;
                        if (requested > available) {
                                String productName = productOpt.get().getNombre();
                                errors.put(pid, String.format(
                                        "'%s': solicitas %d unidades pero solo hay %d disponibles para esas fechas (stock total: %d, ya reservadas: %d).",
                                        productName, requested, Math.max(available, 0), totalStock, maxReserved));
                        }
                }
                return errors;
        }

        public void updateStatus(String id, String newStatus) {
                updateStatus(id, newStatus, "Sistema", null);
        }

        public void updateStatus(String id, String newStatus, String usuario, String nota) {
                Reservation r = getByIdOrThrow(id);
                String oldStatus = r.getEstado();

                // Validar transición (CANCELADA se permite desde cualquier estado)
                if (!EstadoReserva.CANCELADA.name().equals(newStatus)) {
                        Set<String> allowed = TRANSITIONS.getOrDefault(oldStatus, Set.of());
                        if (!allowed.contains(newStatus)) {
                                throw new InvalidOperationException(
                                                String.format("Transición no válida: %s → %s", oldStatus, newStatus));
                        }
                }

                r.setEstado(newStatus);
                r.setFechaActualizacion(java.time.LocalDateTime.now());
                reservationRepository.save(r);

                // Registrar en historial
                statusHistoryRepository.save(new StatusHistory(id, oldStatus, newStatus, usuario, nota));
                log.info("Reserva {} cambió de {} a {} por {}", id, oldStatus, newStatus, usuario);

                // Publicar evento si fue cancelada
                if (EstadoReserva.CANCELADA.name().equals(newStatus)) {
                        metricsConfig.getReservationsCancelled().increment();
                        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant()
                                        : "default";
                        eventPublisher.publish(new ReservationCancelledEvent(this, r, tenantId,
                                        nota != null ? nota : "Sin razón especificada"));
                }
        }

        public List<StatusHistory> getStatusHistory(String reservaId) {
                return statusHistoryRepository.findByReservaIdOrderByFechaAsc(reservaId);
        }

        public long count() {
                return reservationRepository.count();
        }

        public long countByEstado(String estado) {
                return reservationRepository.countByEstado(estado);
        }

        public BigDecimal calculateTotalRevenue() {
                return reservationRepository.findAll().stream()
                                .filter(r -> EstadoReserva.COMPLETADA.name().equals(r.getEstado())
                                                || EstadoReserva.ACTIVA.name().equals(r.getEstado())
                                                || EstadoReserva.CONFIRMADA.name().equals(r.getEstado()))
                                .map(Reservation::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public Map<String, BigDecimal> getRevenueByDay() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");
                return reservationRepository.findAll().stream()
                                .filter(r -> r.getFechaCreacion() != null && r.getTotal() != null)
                                .collect(Collectors.groupingBy(
                                                r -> r.getFechaCreacion().toLocalDate().format(formatter),
                                                Collectors.reducing(BigDecimal.ZERO, Reservation::getTotal, BigDecimal::add)));
        }

        public Map<String, Long> getStatusDistribution() {
                return reservationRepository.findAll().stream()
                                .collect(Collectors.groupingBy(Reservation::getEstado, Collectors.counting()));
        }

        public List<Reservation> getRecentActivity() {
                return reservationRepository.findAll().stream()
                                .sorted((r1, r2) -> r2.getFechaCreacion().compareTo(r1.getFechaCreacion()))
                                .limit(5)
                                .collect(Collectors.toList());
        }

        public List<String> getReservedDatesForProducts(List<String> productIds,
                        com.alquiler.furent.repository.ProductRepository productRepository) {
                List<Reservation> activeReservations = reservationRepository.findAll().stream()
                                .filter(r -> "ACTIVA".equals(r.getEstado()) || "CONFIRMADA".equals(r.getEstado()))
                                .collect(Collectors.toList());

                java.util.Set<String> blockedDates = new java.util.HashSet<>();
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

                Map<String, Integer> productTotalStocks = new java.util.HashMap<>();
                for (String pid : productIds) {
                        productRepository.findById(pid).ifPresent(p -> productTotalStocks.put(pid, p.getStock()));
                }

                Map<String, Map<String, Integer>> dailyUsage = new java.util.HashMap<>();

                for (Reservation res : activeReservations) {
                        if (res.getFechaInicio() != null && res.getFechaFin() != null) {
                                java.time.LocalDate start = res.getFechaInicio();
                                java.time.LocalDate end = res.getFechaFin();

                                while (!start.isAfter(end)) {
                                        String dateStr = start.format(formatter);
                                        dailyUsage.putIfAbsent(dateStr, new java.util.HashMap<>());
                                        Map<String, Integer> usageThatDay = dailyUsage.get(dateStr);

                                        for (Reservation.ItemReserva item : res.getItems()) {
                                                if (productIds.contains(item.getProductoId())) {
                                                        usageThatDay.put(item.getProductoId(),
                                                                        usageThatDay.getOrDefault(item.getProductoId(),
                                                                                        0) + item.getCantidad());
                                                }
                                        }
                                        start = start.plusDays(1);
                                }
                        }
                }

                for (Map.Entry<String, Map<String, Integer>> entry : dailyUsage.entrySet()) {
                        String dateStr = entry.getKey();
                        Map<String, Integer> usageThatDay = entry.getValue();

                        for (String pid : productIds) {
                                int limit = productTotalStocks.getOrDefault(pid, 999999);
                                if (usageThatDay.getOrDefault(pid, 0) >= limit) {
                                        blockedDates.add(dateStr);
                                        break;
                                }
                        }
                }

                return new java.util.ArrayList<>(blockedDates);
        }
}
