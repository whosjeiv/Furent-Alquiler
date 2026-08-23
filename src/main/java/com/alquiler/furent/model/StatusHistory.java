package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "estado_historial")
public class StatusHistory {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    private String reservaId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String usuarioAccion;
    private String nota;
    private LocalDateTime fecha;

    public StatusHistory() {
        this.fecha = LocalDateTime.now();
    }

    public StatusHistory(String reservaId, String estadoAnterior, String estadoNuevo, String usuarioAccion, String nota) {
        this();
        this.reservaId = reservaId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuarioAccion = usuarioAccion;
        this.nota = nota;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReservaId() { return reservaId; }
    public void setReservaId(String reservaId) { this.reservaId = reservaId; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getUsuarioAccion() { return usuarioAccion; }
    public void setUsuarioAccion(String usuarioAccion) { this.usuarioAccion = usuarioAccion; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
