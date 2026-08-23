package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "combos")
public class Combo {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private List<ComboItem> items = new ArrayList<>();
    private BigDecimal precioOriginal; // sum of individual item prices × quantities
    private BigDecimal precioCombo;    // discounted price
    private double porcentajeDescuento;
    private boolean activo;

    public Combo() {
    }

    // Inner class for combo items
    public static class ComboItem {
        private String productoId;
        private String productoNombre;
        private String productoImagen;
        private BigDecimal precioPorDia;
        private int cantidad;

        public ComboItem() {
        }

        public ComboItem(String productoId, String productoNombre, String productoImagen,
                BigDecimal precioPorDia, int cantidad) {
            this.productoId = productoId;
            this.productoNombre = productoNombre;
            this.productoImagen = productoImagen;
            this.precioPorDia = precioPorDia;
            this.cantidad = cantidad;
        }

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }

        public String getProductoNombre() { return productoNombre; }
        public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

        // Thymeleaf alias
        public String getProductName() { return productoNombre; }

        public String getProductoImagen() { return productoImagen; }
        public void setProductoImagen(String productoImagen) { this.productoImagen = productoImagen; }

        public String getProductImage() { return productoImagen; }

        public BigDecimal getPrecioPorDia() { return precioPorDia; }
        public void setPrecioPorDia(BigDecimal precioPorDia) { this.precioPorDia = precioPorDia; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public int getQuantity() { return cantidad; }
    }

    // === Getters & Setters ===

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getName() { return nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDescription() { return descripcion; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getImageUrl() { return imagenUrl; }
    public String getImage() { return imagenUrl; }

    public List<ComboItem> getItems() { return items; }
    public void setItems(List<ComboItem> items) { this.items = items; }

    public BigDecimal getPrecioOriginal() { return precioOriginal; }
    public void setPrecioOriginal(BigDecimal precioOriginal) { this.precioOriginal = precioOriginal; }
    public BigDecimal getOriginalPrice() { return precioOriginal; }

    public BigDecimal getPrecioCombo() { return precioCombo; }
    public void setPrecioCombo(BigDecimal precioCombo) { this.precioCombo = precioCombo; }
    public BigDecimal getComboPrice() { return precioCombo; }

    public double getPorcentajeDescuento() { return porcentajeDescuento; }
    public void setPorcentajeDescuento(double porcentajeDescuento) { this.porcentajeDescuento = porcentajeDescuento; }
    public double getDiscountPercent() { return porcentajeDescuento; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isActive() { return activo; }
}
