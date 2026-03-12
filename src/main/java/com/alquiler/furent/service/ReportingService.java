package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.model.AnalyticsEvent;
import com.alquiler.furent.model.ReportCache;
import com.alquiler.furent.repository.AnalyticsEventRepository;
import com.alquiler.furent.repository.ReportCacheRepository;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.PaymentRepository;
import com.alquiler.furent.repository.UserRepository;
import com.alquiler.furent.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de reportería y analytics.
 * Genera reportes de métricas clave, almacena en caché reportes costosos
 * y registra eventos de analytics.
 */
@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final AnalyticsEventRepository analyticsEventRepository;
    private final ReportCacheRepository reportCacheRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReportingService(AnalyticsEventRepository analyticsEventRepository,
                            ReportCacheRepository reportCacheRepository,
                            ReservationRepository reservationRepository,
                            PaymentRepository paymentRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.reportCacheRepository = reportCacheRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    // ===== ANALYTICS EVENTS =====

    public void trackEvent(String eventType, String userId, String entityType, String entityId, Map<String, Object> metadata) {
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        AnalyticsEvent event = new AnalyticsEvent();
        event.setTenantId(tenantId);
        event.setEventType(eventType);
        event.setUserId(userId);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setMetadata(metadata);
        analyticsEventRepository.save(event);
    }

    public List<AnalyticsEvent> getEventsByTenant(String tenantId) {
        return analyticsEventRepository.findByTenantId(tenantId);
    }

    public List<AnalyticsEvent> getEventsByType(String tenantId, String eventType) {
        return analyticsEventRepository.findByTenantIdAndEventType(tenantId, eventType);
    }

    public List<AnalyticsEvent> getEventsByDateRange(String tenantId, LocalDateTime from, LocalDateTime to) {
        return analyticsEventRepository.findByTenantIdAndTimestampBetween(tenantId, from, to);
    }

    public long countEventsByType(String tenantId, String eventType) {
        return analyticsEventRepository.countByTenantIdAndEventType(tenantId, eventType);
    }

    // ===== DASHBOARD REPORT =====

    public Map<String, Object> generateDashboardReport() {
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";

        // Check cache
        Optional<ReportCache> cached = reportCacheRepository.findByTenantIdAndReportType(tenantId, "DASHBOARD");
        if (cached.isPresent() && !cached.get().isExpired()) {
            log.debug("Dashboard report served from cache for tenant: {}", tenantId);
            return cached.get().getData();
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalUsuarios", userRepository.count());
        report.put("totalProductos", productRepository.count());
        report.put("totalReservas", reservationRepository.count());
        report.put("totalPagos", paymentRepository.count());

        // Revenue
        BigDecimal revenue = paymentRepository.findAll().stream()
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("ingresoTotal", revenue);

        // Reservas por estado
        Map<String, Long> reservasPorEstado = reservationRepository.findAll().stream()
                .collect(Collectors.groupingBy(r -> r.getEstado(), Collectors.counting()));
        report.put("reservasPorEstado", reservasPorEstado);

        // Eventos recientes
        report.put("eventosRecientes", analyticsEventRepository.findByTenantId(tenantId).stream()
                .sorted(Comparator.comparing(AnalyticsEvent::getTimestamp).reversed())
                .limit(20)
                .collect(Collectors.toList()));

        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("tenantId", tenantId);

        // Cache for 5 minutes
        ReportCache cache = new ReportCache(tenantId, "DASHBOARD", report, LocalDateTime.now().plusMinutes(5));
        reportCacheRepository.save(cache);

        log.info("Dashboard report generated for tenant: {}", tenantId);
        return report;
    }

    // ===== REVENUE REPORT =====

    public Map<String, Object> generateRevenueReport(LocalDateTime from, LocalDateTime to) {
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tenantId", tenantId);
        report.put("periodo", Map.of("desde", from.toString(), "hasta", to.toString()));

        var pagos = paymentRepository.findAll().stream()
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .filter(p -> p.getFechaPago() != null && !p.getFechaPago().isBefore(from) && !p.getFechaPago().isAfter(to))
                .collect(Collectors.toList());

        report.put("totalPagos", pagos.size());
        report.put("montoTotal", pagos.stream()
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Por método de pago
        Map<String, BigDecimal> porMetodo = pagos.stream()
                .collect(Collectors.groupingBy(p -> p.getMetodoPago() != null ? p.getMetodoPago() : "DESCONOCIDO",
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO, BigDecimal::add)));
        report.put("porMetodoPago", porMetodo);

        report.put("generatedAt", LocalDateTime.now().toString());
        return report;
    }

    // ===== INVALIDATE CACHE =====

    public void invalidateCache(String reportType) {
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        reportCacheRepository.deleteByTenantIdAndReportType(tenantId, reportType);
        log.info("Cache invalidated: type={} tenant={}", reportType, tenantId);
    }
}
