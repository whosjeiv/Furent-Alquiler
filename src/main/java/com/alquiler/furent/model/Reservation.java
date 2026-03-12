package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "reservas")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_usuario", def = "{'tenantId': 1, 'usuarioId': 1}"),
    @CompoundIndex(name = "idx_tenant_estado", def = "{'tenantId': 1, 'estado': 1}"),
    @CompoundIndex(name = "idx_usuario_estado", def = "{'usuarioId': 1, 'estado': 1}")
})
public class Reservation {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    private String usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private List<ItemReserva> items = new ArrayList<>();
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int diasAlquiler;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private String codigoCupon;
    private BigDecimal total;
    private String estado; // PENDIENTE, CONFIRMADA, ACTIVA, COMPLETADA, CANCELADA
    private String metodoPago;
    private String direccionEvento;
    private String notasEvento;
    private String tipoEvento;
    @Indexed
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Reservation() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }

    // Inner class for reservation items
    public static class ItemReserva {
        private String productoId;
        private String productoNombre;
        private String productoImagen;
        private BigDecimal precioPorDia;
        private int cantidad;
        private BigDecimal subtotal;

        public ItemReserva() {
        }

        public ItemReserva(String productoId, String productoNombre, String productoImagen,
                BigDecimal precioPorDia, int cantidad) {
            this.productoId = productoId;
            this.productoNombre = productoNombre;
            this.productoImagen = productoImagen;
            this.precioPorDia = precioPorDia;
            this.cantidad = cantidad;
            this.subtotal = precioPorDia.multiply(BigDecimal.valueOf(cantidad));
        }

        public String getProductoId() {
            return productoId;
        }

        public void setProductoId(String productoId) {
            this.productoId = productoId;
        }

        public String getProductoNombre() {
            return productoNombre;
        }

        public void setProductoNombre(String productoNombre) {
            this.productoNombre = productoNombre;
        }

        // Alias
        public String getProductName() {
            return productoNombre;
        }

        public String getProductoImagen() {
            return productoImagen;
        }

        public void setProductoImagen(String productoImagen) {
            this.productoImagen = productoImagen;
        }

        public String getProductImage() {
            return productoImagen;
        }

        public BigDecimal getPrecioPorDia() {
            return precioPorDia;
        }

        public void setPrecioPorDia(BigDecimal precioPorDia) {
            this.precioPorDia = precioPorDia;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }

        public int getQuantity() {
            return cantidad;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public void setUsuarioEmail(String usuarioEmail) {
        this.usuarioEmail = usuarioEmail;
    }

    public List<ItemReserva> getItems() {
        return items;
    }

    public void setItems(List<ItemReserva> items) {
        this.items = items;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getStartDate() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public LocalDate getEndDate() {
        return fechaFin;
    }

    public int getDiasAlquiler() {
        return diasAlquiler;
    }

    public void setDiasAlquiler(int diasAlquiler) {
        this.diasAlquiler = diasAlquiler;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public String getCodigoCupon() {
        return codigoCupon;
    }

    public void setCodigoCupon(String codigoCupon) {
        this.codigoCupon = codigoCupon;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotalPrice() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getStatus() {
        return estado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getDireccionEvento() {
        return direccionEvento;
    }

    public void setDireccionEvento(String direccionEvento) {
        this.direccionEvento = direccionEvento;
    }

    public String getNotasEvento() {
        return notasEvento;
    }

    public void setNotasEvento(String notasEvento) {
        this.notasEvento = notasEvento;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
