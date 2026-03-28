package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_usuario", def = "{'tenantId': 1, 'usuarioId': 1}"),
    @CompoundIndex(name = "idx_tenant_estado", def = "{'tenantId': 1, 'estado': 1}"),
    @CompoundIndex(name = "idx_reserva", def = "{'reservaId': 1}")
})
public class Payment {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    
    @Indexed
    private String reservaId;
    
    @Indexed
    private String usuarioId;
    
    private BigDecimal monto;
    
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, TARJETA
    
    @Indexed
    private String estado; // PENDIENTE, PAGADO, FALLIDO
    
    private String referencia; // PAY-XXXXXXXX
    
    private LocalDateTime fechaCreacion;
    
    private LocalDateTime fechaPago; // Cuando se confirma
    
    public Payment() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }

    // Getters & Setters
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getReservaId() {
        return reservaId;
    }

    public void setReservaId(String reservaId) {
        this.reservaId = reservaId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
}
