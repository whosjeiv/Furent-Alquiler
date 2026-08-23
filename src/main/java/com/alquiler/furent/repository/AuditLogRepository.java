package com.alquiler.furent.repository;

import com.alquiler.furent.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop10ByOrderByFechaDesc();
    List<AuditLog> findByEntidadOrderByFechaDesc(String entidad);
    List<AuditLog> findByUsuarioOrderByFechaDesc(String usuario);
    List<AuditLog> findByAccionOrderByFechaDesc(String accion);
    List<AuditLog> findByTenantIdOrderByFechaDesc(String tenantId);
    List<AuditLog> findBySeverityOrderByFechaDesc(String severity);
    List<AuditLog> findByFechaBetweenOrderByFechaDesc(LocalDateTime start, LocalDateTime end);
    long countByAccion(String accion);
}
