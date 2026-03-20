package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "usuarios")
@CompoundIndex(name = "tenant_email_idx", def = "{'tenantId': 1, 'email': 1}", unique = true)
public class User {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String nombre;
    private String apellido;
    private String telefono;
    private String role; // USER, ADMIN
    private LocalDateTime fechaCreacion;
    private boolean activo;
    private String razonSuspension;
    private LocalDateTime fechaInicioSuspension;
    private LocalDateTime fechaFinSuspension;
    private boolean suspensionPermanente;

    // Favoritos (IDs de productos)
    private List<String> favoritos = new ArrayList<>();

    // Preferencias de usuario
    private String idioma = "es";
    private String moneda = "COP";
    private String apariencia = "light";
    private boolean notificacionesEmail = true;

    // 2FA (Google Authenticator)
    private String totpSecret;
    private boolean totpEnabled = false;

    public User() {
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
        this.role = "USER";
    }

    public User(String email, String password, String nombre, String apellido, String telefono, String role) {
        this();
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.role = role;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getRazonSuspension() {
        return razonSuspension;
    }

    public void setRazonSuspension(String razonSuspension) {
        this.razonSuspension = razonSuspension;
    }

    public LocalDateTime getFechaFinSuspension() {
        return fechaFinSuspension;
    }

    public void setFechaFinSuspension(LocalDateTime fechaFinSuspension) {
        this.fechaFinSuspension = fechaFinSuspension;
    }

    public LocalDateTime getFechaInicioSuspension() {
        return fechaInicioSuspension;
    }

    public void setFechaInicioSuspension(LocalDateTime fechaInicioSuspension) {
        this.fechaInicioSuspension = fechaInicioSuspension;
    }

    public boolean isSuspensionPermanente() {
        return suspensionPermanente;
    }

    public void setSuspensionPermanente(boolean suspensionPermanente) {
        this.suspensionPermanente = suspensionPermanente;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getApariencia() {
        return apariencia;
    }

    public void setApariencia(String apariencia) {
        this.apariencia = apariencia;
    }

    public List<String> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(List<String> favoritos) {
        this.favoritos = favoritos;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isNotificacionesEmail() {
        return notificacionesEmail;
    }

    public void setNotificacionesEmail(boolean notificacionesEmail) {
        this.notificacionesEmail = notificacionesEmail;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }
}
