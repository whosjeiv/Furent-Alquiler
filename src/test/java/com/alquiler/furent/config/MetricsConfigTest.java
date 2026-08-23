package com.alquiler.furent.config;

import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReservationRepository;
import com.alquiler.furent.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsConfigTest {

    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReservationRepository reservationRepository;

    private MetricsConfig metricsConfig;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        metricsConfig = new MetricsConfig(registry, productRepository, userRepository, reservationRepository);
    }

    @Test
    void counters_initializedCorrectly() {
        assertNotNull(metricsConfig.getReservationsCreated());
        assertNotNull(metricsConfig.getReservationsCancelled());
        assertNotNull(metricsConfig.getPaymentsCompleted());
        assertNotNull(metricsConfig.getPaymentsFailed());
        assertNotNull(metricsConfig.getUsersRegistered());
        assertNotNull(metricsConfig.getReviewsCreated());
    }

    @Test
    void timers_initializedCorrectly() {
        assertNotNull(metricsConfig.getReservationProcessingTime());
        assertNotNull(metricsConfig.getPaymentProcessingTime());
    }

    @Test
    void reservationsCreated_counter_increments() {
        Counter counter = metricsConfig.getReservationsCreated();
        double before = counter.count();

        counter.increment();

        assertEquals(before + 1.0, counter.count());
    }

    @Test
    void paymentsFailed_counter_increments() {
        Counter counter = metricsConfig.getPaymentsFailed();
        counter.increment();
        counter.increment();

        assertEquals(2.0, counter.count());
    }

    @Test
    void addRevenue_accumulatesCorrectly() {
        // Use SimpleMeterRegistry to verify gauge behavior
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MetricsConfig config = new MetricsConfig(registry, productRepository, userRepository, reservationRepository);

        config.addRevenue(BigDecimal.valueOf(100000));
        config.addRevenue(BigDecimal.valueOf(50000));

        // Verify the gauge is accessible
        assertNotNull(registry.find("furent.revenue.total").gauge());
        assertEquals(150000.0, registry.find("furent.revenue.total").gauge().value());
    }

    @Test
    void reservationProcessingTime_recordsDuration() {
        Timer timer = metricsConfig.getReservationProcessingTime();
        timer.record(java.time.Duration.ofMillis(100));

        assertEquals(1, timer.count());
    }

    @Test
    void paymentProcessingTime_recordsDuration() {
        Timer timer = metricsConfig.getPaymentProcessingTime();
        timer.record(java.time.Duration.ofMillis(200));
        timer.record(java.time.Duration.ofMillis(300));

        assertEquals(2, timer.count());
    }
}
