package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.model.AuditLog;
import com.alquiler.furent.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

/**
 * Servicio de auditoría avanzada y trazabilidad.
 * Registra acciones críticas del sistema con contexto enriquecido:
 * IP, User-Agent, método HTTP, path, metadata y severidad.
 *
 * @author Furent Team
 * @since 2.0
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registro básico (compatibilidad retroactiva).
     */
    public void log(String usuario, String accion, String entidad, String entidadId, String detalle) {
        AuditLog auditLog = new AuditLog(usuario, accion, entidad, entidadId, detalle);
        enrichWithRequestContext(auditLog);
        auditLog.setTenantId(TenantContext.getCurrentTenant());
        auditLogRepository.save(auditLog);
        log.info("AUDIT: usuario={}, accion={}, entidad={}, id={}", usuario, accion, entidad, entidadId);
    }

    /**
     * Registro avanzado con severidad y metadata adicional.
     */
    public void logAdvanced(String usuario, String accion, String entidad, String entidadId,
                            String detalle, String severity, Map<String, Object> metadata) {
        AuditLog auditLog = new AuditLog(usuario, accion, entidad, entidadId, detalle);
        auditLog.setSeverity(severity);
        if (metadata != null) {
            auditLog.setMetadata(metadata);
        }
        enrichWithRequestContext(auditLog);
        auditLog.setTenantId(TenantContext.getCurrentTenant());
        auditLogRepository.save(auditLog);
        log.info("AUDIT [{}]: usuario={}, accion={}, entidad={}, id={}",
                severity, usuario, accion, entidad, entidadId);
    }

    /**
     * Registro de evento crítico de seguridad.
     */
    public void logSecurity(String usuario, String accion, String detalle) {
        logAdvanced(usuario, accion, "SEGURIDAD", null, detalle, "CRITICAL", null);
    }

    /**
     * Enriquece el audit log con contexto del request HTTP actual.
     */
    private void enrichWithRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIpAddress(extractClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestPath(request.getRequestURI());
            }
        } catch (Exception e) {
            // Si no hay contexto HTTP (e.g., evento async), se ignora silenciosamente
            log.debug("No se pudo enriquecer audit log con contexto HTTP: {}", e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop10ByOrderByFechaDesc();
    }

    public List<AuditLog> getLogsByUsuario(String usuario) {
        return auditLogRepository.findByUsuarioOrderByFechaDesc(usuario);
    }

    public List<AuditLog> getLogsByAccion(String accion) {
        return auditLogRepository.findByAccionOrderByFechaDesc(accion);
    }
}
