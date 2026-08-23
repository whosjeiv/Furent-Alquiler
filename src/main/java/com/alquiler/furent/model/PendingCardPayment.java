package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cotización pendiente de pago con tarjeta.
 */
@Document(collection = "pagos_pendientes_v2")
public class PendingCardPayment {

    @Id
    private String id;

    private String tenantId;
    private String usuarioId;
    /** JSON del objeto Reservation (sin id) para recrear la reserva al confirmar el pago. */
    private String reservationDataJson;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;

    public PendingCardPayment() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getReservationDataJson() { return reservationDataJson; }
    public void setReservationDataJson(String reservationDataJson) { this.reservationDataJson = reservationDataJson; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
