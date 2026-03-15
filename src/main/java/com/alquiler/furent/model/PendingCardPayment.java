package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cotización pendiente de pago con tarjeta.
 * La reserva solo se crea cuando Stripe confirma el pago (webhook).
 */
@Document(collection = "pagos_tarjeta_pendientes")
public class PendingCardPayment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String stripePaymentIntentId;

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
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
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
