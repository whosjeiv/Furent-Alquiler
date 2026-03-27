package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Version
    private Long version; // Optimistic locking: previene race conditions en writes concurrentes

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
    private String estado; // PENDIENTE, CONFIRMADA, ENTREGADA, COMPLETADA, CANCELADA
    private String metodoPago;
    private String direccionEvento;
    private String notasEvento;
    private String tipoEvento;
    private String horaEntrega; // HH:mm — hora preferida de entrega logística
    /** Código único para pago en efectivo en oficina (ej. FRNT-A1B2C3D4). Se envía por correo. */
    private String codigoPagoEfectivo;
    /** Monto total abonado por el cliente (suma de pagos confirmados). */
    private BigDecimal montoAbonado;
    /** Estado financiero: SIN_PAGO, ANTICIPO, PARCIAL, PAGADO */
    private String estadoPago;
    @Indexed
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Reservation() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
        this.montoAbonado = BigDecimal.ZERO;
        this.estadoPago = "SIN_PAGO";
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getHoraEntrega() {
        return horaEntrega;
    }

    public void setHoraEntrega(String horaEntrega) {
        this.horaEntrega = horaEntrega;
    }

    public String getCodigoPagoEfectivo() {
        return codigoPagoEfectivo;
    }

    public void setCodigoPagoEfectivo(String codigoPagoEfectivo) {
        this.codigoPagoEfectivo = codigoPagoEfectivo;
    }

    public BigDecimal getMontoAbonado() {
        return montoAbonado;
    }

    public void setMontoAbonado(BigDecimal montoAbonado) {
        this.montoAbonado = montoAbonado;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    /** Saldo pendiente = total - montoAbonado (nunca negativo). */
    public BigDecimal getSaldoPendiente() {
        BigDecimal abonado = montoAbonado != null ? montoAbonado : BigDecimal.ZERO;
        BigDecimal totalVal = total != null ? total : BigDecimal.ZERO;
        return totalVal.subtract(abonado).max(BigDecimal.ZERO);
    }

    /** Porcentaje pagado (0-100) redondeado hacia abajo. */
    public int getPorcentajePagado() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal abonado = montoAbonado != null ? montoAbonado : BigDecimal.ZERO;
        return abonado.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.FLOOR).intValue();
    }
}
