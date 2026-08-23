package com.alquiler.furent.service;

import com.alquiler.furent.model.AuditLog;
import com.alquiler.furent.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void log_savesAuditLogWithCorrectFields() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.log("admin", "CREATE", "PRODUCT", "prod-1", "Producto creado");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("admin", saved.getUsuario());
        assertEquals("CREATE", saved.getAccion());
        assertEquals("PRODUCT", saved.getEntidad());
        assertEquals("prod-1", saved.getEntidadId());
        assertEquals("Producto creado", saved.getDetalle());
    }

    @Test
    void logAdvanced_saveWithSeverityAndMetadata() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));
        Map<String, Object> metadata = Map.of("key", "value");

        auditLogService.logAdvanced("user1", "DELETE", "RESERVATION", "res-1",
                "Reserva eliminada", "WARNING", metadata);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("WARNING", saved.getSeverity());
        assertEquals("value", saved.getMetadata().get("key"));
    }

    @Test
    void logSecurity_savesCriticalSeverity() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logSecurity("SISTEMA", "BRUTE_FORCE", "IP bloqueada");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("CRITICAL", saved.getSeverity());
        assertEquals("SEGURIDAD", saved.getEntidad());
        assertEquals("BRUTE_FORCE", saved.getAccion());
    }

    @Test
    void getRecentLogs_delegatesToRepository() {
        AuditLog log1 = new AuditLog("u", "a", "e", "1", "d");
        when(auditLogRepository.findTop10ByOrderByFechaDesc()).thenReturn(List.of(log1));

        List<AuditLog> result = auditLogService.getRecentLogs();

        assertEquals(1, result.size());
        verify(auditLogRepository).findTop10ByOrderByFechaDesc();
    }

    @Test
    void getLogsByUsuario_delegatesToRepository() {
        when(auditLogRepository.findByUsuarioOrderByFechaDesc("admin")).thenReturn(List.of());

        List<AuditLog> result = auditLogService.getLogsByUsuario("admin");

        assertTrue(result.isEmpty());
        verify(auditLogRepository).findByUsuarioOrderByFechaDesc("admin");
    }

    @Test
    void getLogsByAccion_delegatesToRepository() {
        when(auditLogRepository.findByAccionOrderByFechaDesc("LOGIN")).thenReturn(List.of());

        List<AuditLog> result = auditLogService.getLogsByAccion("LOGIN");

        assertTrue(result.isEmpty());
        verify(auditLogRepository).findByAccionOrderByFechaDesc("LOGIN");
    }
}
