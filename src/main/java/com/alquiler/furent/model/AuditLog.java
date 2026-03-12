package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Registro de auditoría avanzado para trazabilidad completa.
 * Almacena cada acción relevante del sistema con contexto enriquecido.
 *
 * Campos clave según arquitectura 10/10:
 * - userId, action, timestamp, ipAddress, tenantId, metadata
 */
@Document(collection = "audit_logs")
@CompoundIndex(name = "tenant_fecha_idx", def = "{'tenantId': 1, 'fecha': -1}")
@CompoundIndex(name = "usuario_accion_idx", def = "{'usuario': 1, 'accion': 1}")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    private String usuario;
    private String accion;
    private String entidad;
    private String entidadId;
    private String detalle;
    private LocalDateTime fecha;

    // === Campos nuevos de auditoría avanzada ===
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestPath;
    private String severity; // INFO, WARNING, CRITICAL
    private Map<String, Object> metadata = new HashMap<>();

    public AuditLog() {
        this.fecha = LocalDateTime.now();
        this.severity = "INFO";
    }

    public AuditLog(String usuario, String accion, String entidad, String entidadId, String detalle) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.fecha = LocalDateTime.now();
        this.severity = "INFO";
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(String entidadId) {
        this.entidadId = entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
