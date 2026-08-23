package com.alquiler.furent.scheduler;

import com.alquiler.furent.enums.EstadoReserva;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.service.ReservationService;
import com.alquiler.furent.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job programado para limpieza automática de reservas.
 * 
 * Funciones:
 * 1. Cancela reservas PENDIENTE que llevan más de 48h sin confirmar
 *    (libera inventario bloqueado innecesariamente).
 * 
 * Se ejecuta cada hora para mantener el inventario limpio.
 *
 * @author Furent Team
 * @since 1.1
 */
@Component
public class ReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);

    /**
     * Tiempo máximo (en horas) que una reserva puede permanecer en estado PENDIENTE
     * antes de ser cancelada automáticamente.
     */
    private static final int PENDING_TIMEOUT_HOURS = 48;

    private final ReservationService reservationService;
    private final NotificationService notificationService;

    public ReservationCleanupScheduler(ReservationService reservationService,
                                        NotificationService notificationService) {
        this.reservationService = reservationService;
        this.notificationService = notificationService;
    }

    /**
     * Se ejecuta cada hora. Busca reservas PENDIENTE creadas hace más de 48h
     * y las cancela automáticamente, liberando el stock bloqueado.
     */
    @Scheduled(fixedRate = 3600000) // Cada 1 hora (en milisegundos)
    public void cancelStalePendingReservations() {
        log.info("🔄 Ejecutando limpieza de reservas PENDIENTE stale...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(PENDING_TIMEOUT_HOURS);

        List<Reservation> pendientes = reservationService.getPendingReservations();

        int cancelledCount = 0;
        for (Reservation r : pendientes) {
            if (r.getFechaCreacion() != null && r.getFechaCreacion().isBefore(cutoff)) {
                try {
                    reservationService.updateStatus(
                            r.getId(),
                            EstadoReserva.CANCELADA.name(),
                            "SISTEMA",
                            "Cancelada automáticamente por inactividad (más de " + PENDING_TIMEOUT_HOURS + "h sin confirmar)"
                    );

                    // Notificar al usuario
                    if (r.getUsuarioId() != null) {
                        notificationService.notify(
                                r.getUsuarioId(),
                                "Reserva Cancelada Automáticamente",
                                "Tu cotización #" + r.getId().substring(0, Math.min(8, r.getId().length()))
                                        + " fue cancelada porque no fue confirmada en " + PENDING_TIMEOUT_HOURS + " horas. "
                                        + "Puedes crear una nueva cotización desde el catálogo.",
                                "WARNING",
                                "/catalogo"
                        );
                    }

                    cancelledCount++;
                    log.info("Reserva {} cancelada automáticamente (creada: {})", r.getId(), r.getFechaCreacion());
                } catch (Exception e) {
                    log.error("Error al cancelar reserva stale {}: {}", r.getId(), e.getMessage());
                }
            }
        }

        if (cancelledCount > 0) {
            log.info("✅ Limpieza completada: {} reservas PENDIENTE canceladas por inactividad", cancelledCount);
        } else {
            log.debug("Sin reservas PENDIENTE stale para limpiar");
        }
    }
}
