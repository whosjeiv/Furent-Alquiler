package com.alquiler.furent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
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

    // ===== NUEVOS CAMPOS DE PERFIL Y CONTACTO =====
    private String tipoDocumento; // CC, CE, NIT, Pasaporte
    private String documentoIdentidad;
    private java.time.LocalDate fechaNacimiento;
    private String genero;
    private String empresa;
    private String cargo;

    // ===== DIRECCIÓN =====
    private String direccion;
    private String ciudad;
    private String estadoProvincia;
    private String codigoPostal;
    private String pais;
    private String estadoCivil;

    // Lista de direcciones guardadas
    private List<UserAddress> direcciones = new ArrayList<>();

    // ===== METADATOS Y ADMINISTRACIÓN =====
    private LocalDateTime ultimaSesion;
    private String notasAdmin;

    // Preferencias de usuario
    private String idioma = "es";
    private String moneda = "COP";
    private String apariencia = "light";
    private boolean notificacionesEmail = true;

    // 2FA (Google Authenticator)
    @JsonIgnore
    private String totpSecret;
    private boolean totpEnabled = false;

    // OAuth2 fields
    private String provider; // "local", "google", "facebook", etc.
    private String providerId; // ID del usuario en el proveedor OAuth
    private String profileImageUrl;

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

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public java.time.LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(java.time.LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEstadoProvincia() {
        return estadoProvincia;
    }

    public void setEstadoProvincia(String estadoProvincia) {
        this.estadoProvincia = estadoProvincia;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public List<UserAddress> getDirecciones() {
        return direcciones;
    }

    public void setDirecciones(List<UserAddress> direcciones) {
        this.direcciones = direcciones;
    }

    public LocalDateTime getUltimaSesion() {
        return ultimaSesion;
    }

    public void setUltimaSesion(LocalDateTime ultimaSesion) {
        this.ultimaSesion = ultimaSesion;
    }

    public String getNotasAdmin() {
        return notasAdmin;
    }

    public void setNotasAdmin(String notasAdmin) {
        this.notasAdmin = notasAdmin;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
