package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.util.List;

@Document(collection = "mobiliarios")
public class Product {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    private String nombre;
    private String descripcion;
    private String descripcionCorta;
    private BigDecimal precioPorDia;
    private String imagenUrl;
    private String categoriaId;
    private String categoriaNombre;
    private double calificacion;
    private int cantidadResenas;
    private boolean disponible;
    private String material;
    private String dimensiones;
    private String color;
    private int cantidadMinima;
    private int cantidadMaxima;
    private int stock;
    private int stockMinimo; // Para alertas de bajo stock
    private String estadoMantenimiento; // EXCELENTE, BUENO, REGULAR, EN_REPARACION
    private String notasMantenimiento;
    private List<String> galleryImages;

    public Product() {
    }

    public Product(String nombre, String descripcion, String descripcionCorta,
            BigDecimal precioPorDia, String imagenUrl, String categoriaId, String categoriaNombre,
            double calificacion, int cantidadResenas, boolean disponible,
            String material, String dimensiones, String color,
            int cantidadMinima, int cantidadMaxima, int stock, int stockMinimo,
            String estadoMantenimiento) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.descripcionCorta = descripcionCorta;
        this.precioPorDia = precioPorDia;
        this.imagenUrl = imagenUrl;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.calificacion = calificacion;
        this.cantidadResenas = cantidadResenas;
        this.disponible = disponible;
        this.material = material;
        this.dimensiones = dimensiones;
        this.color = color;
        this.cantidadMinima = cantidadMinima;
        this.cantidadMaxima = cantidadMaxima;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.estadoMantenimiento = estadoMantenimiento;
    }

    // === Getters & Setters ===
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Thymeleaf compat aliases
    public String getName() {
        return nombre;
    }

    public void setName(String name) {
        this.nombre = name;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescription() {
        return descripcion;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }

    public String getShortDescription() {
        return descripcionCorta;
    }

    public BigDecimal getPrecioPorDia() {
        return precioPorDia;
    }

    public void setPrecioPorDia(BigDecimal precioPorDia) {
        this.precioPorDia = precioPorDia;
    }

    public BigDecimal getPricePerDay() {
        return precioPorDia;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getImageUrl() {
        return imagenUrl;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public String getCategory() {
        return categoriaNombre;
    }

    public double getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(double calificacion) {
        this.calificacion = calificacion;
    }

    public double getRating() {
        return calificacion;
    }

    public int getCantidadResenas() {
        return cantidadResenas;
    }

    public void setCantidadResenas(int cantidadResenas) {
        this.cantidadResenas = cantidadResenas;
    }

    public int getReviewCount() {
        return cantidadResenas;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public boolean isAvailable() {
        return disponible;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(String dimensiones) {
        this.dimensiones = dimensiones;
    }

    public String getDimensions() {
        return dimensiones;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getCantidadMinima() {
        return cantidadMinima;
    }

    public void setCantidadMinima(int cantidadMinima) {
        this.cantidadMinima = cantidadMinima;
    }

    public int getMinQuantity() {
        return cantidadMinima;
    }

    public int getCantidadMaxima() {
        return cantidadMaxima;
    }

    public void setCantidadMaxima(int cantidadMaxima) {
        this.cantidadMaxima = cantidadMaxima;
    }

    public int getMaxQuantity() {
        return cantidadMaxima;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getEstadoMantenimiento() {
        return estadoMantenimiento != null ? estadoMantenimiento : "EXCELENTE";
    }

    public void setEstadoMantenimiento(String estadoMantenimiento) {
        this.estadoMantenimiento = estadoMantenimiento;
    }

    public String getNotasMantenimiento() {
        return notasMantenimiento;
    }

    public void setNotasMantenimiento(String notasMantenimiento) {
        this.notasMantenimiento = notasMantenimiento;
    }

    public boolean isBajoStock() {
        return stock <= stockMinimo;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<String> galleryImages) {
        this.galleryImages = galleryImages;
    }

    public String getImage() {
        return imagenUrl;
    }
}
