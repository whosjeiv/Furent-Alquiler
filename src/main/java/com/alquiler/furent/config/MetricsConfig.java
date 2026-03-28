package com.alquiler.furent.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.UserRepository;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Métricas de negocio personalizadas expuestas a Prometheus/Grafana.
 *
 * Métricas registradas:
 * - furent.reservations.created (Counter): Total de reservas creadas
 * - furent.reservations.cancelled (Counter): Total de reservas canceladas
 * - furent.payments.created (Counter): Total de pagos creados
 * - furent.payments.completed (Counter): Total de pagos completados
 * - furent.payments.failed (Counter): Total de pagos fallidos
 * - furent.users.registered (Counter): Total de registros de usuario
 * - furent.products.total (Gauge): Productos en catálogo
 * - furent.users.total (Gauge): Usuarios registrados
 * - furent.reservations.count (Gauge): Total reservas en sistema
 * - furent.api.request.duration (Timer): Latencia de operaciones críticas
 * - furent.revenue.total (Gauge): Ingresos acumulados en tiempo real
 */
@Configuration
public class MetricsConfig {

    // === Counters ===
    private final Counter reservationsCreated;
    private final Counter reservationsCancelled;
    private final Counter paymentsCreated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter usersRegistered;
    private final Counter reviewsCreated;

    // === Timers ===
    private final Timer reservationProcessingTime;
    private final Timer paymentProcessingTime;

    // === Revenue gauge ===
    private final AtomicReference<BigDecimal> revenueAccumulator = new AtomicReference<>(BigDecimal.ZERO);

    public MetricsConfig(MeterRegistry registry,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         ReservationRepository reservationRepository) {

        // Counters - eventos de negocio
        this.reservationsCreated = Counter.builder("furent.reservations.created")
                .description("Total de reservas creadas")
                .tag("module", "reservations")
                .register(registry);

        this.reservationsCancelled = Counter.builder("furent.reservations.cancelled")
                .description("Total de reservas canceladas")
                .tag("module", "reservations")
                .register(registry);

        this.paymentsCreated = Counter.builder("furent.payments.created")
                .description("Total de pagos creados")
                .tag("module", "payments")
                .register(registry);

        this.paymentsCompleted = Counter.builder("furent.payments.completed")
                .description("Total de pagos completados exitosamente")
                .tag("module", "payments")
                .register(registry);

        this.paymentsFailed = Counter.builder("furent.payments.failed")
                .description("Total de pagos fallidos")
                .tag("module", "payments")
                .register(registry);

        this.usersRegistered = Counter.builder("furent.users.registered")
                .description("Total de usuarios registrados")
                .tag("module", "users")
                .register(registry);

        this.reviewsCreated = Counter.builder("furent.reviews.created")
                .description("Total de reseñas creadas")
                .tag("module", "reviews")
                .register(registry);

        // Gauges - estado actual del sistema
        Gauge.builder("furent.products.total", productRepository, repo -> repo.count())
                .description("Total de productos en catálogo")
                .tag("module", "products")
                .register(registry);

        Gauge.builder("furent.users.total", userRepository, repo -> repo.count())
                .description("Total de usuarios registrados")
                .tag("module", "users")
                .register(registry);

        Gauge.builder("furent.reservations.count", reservationRepository, repo -> repo.count())
                .description("Total de reservas en el sistema")
                .tag("module", "reservations")
                .register(registry);

        // Timers - latencia de operaciones críticas
        this.reservationProcessingTime = Timer.builder("furent.reservation.processing.time")
                .description("Tiempo de procesamiento de reservas")
                .tag("module", "reservations")
                .register(registry);

        this.paymentProcessingTime = Timer.builder("furent.payment.processing.time")
                .description("Tiempo de procesamiento de pagos")
                .tag("module", "payments")
                .register(registry);

        // Gauge de ingresos en tiempo real
        Gauge.builder("furent.revenue.total", revenueAccumulator, ref -> ref.get().doubleValue())
                .description("Ingresos acumulados en tiempo real (COP)")
                .tag("module", "revenue")
                .register(registry);
    }

    /** Acumula ingresos cuando se confirma un pago */
    public void addRevenue(BigDecimal amount) {
        revenueAccumulator.updateAndGet(current -> current.add(amount));
    }

    // === Accessors para inyectar en servicios ===
    public Counter getReservationsCreated() { return reservationsCreated; }
    public Counter getReservationsCancelled() { return reservationsCancelled; }
    public Counter getPaymentsCreated() { return paymentsCreated; }
    public Counter getPaymentsCompleted() { return paymentsCompleted; }
    public Counter getPaymentsFailed() { return paymentsFailed; }
    public Counter getUsersRegistered() { return usersRegistered; }
    public Counter getReviewsCreated() { return reviewsCreated; }
    public Timer getReservationProcessingTime() { return reservationProcessingTime; }
    public Timer getPaymentProcessingTime() { return paymentProcessingTime; }
}
