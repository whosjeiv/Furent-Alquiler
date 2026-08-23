package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "notificaciones")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_leida", def = "{'userId': 1, 'leida': 1, 'fecha': -1}")
})
public class Notification {

    @Id
    private String id;

    private String tenantId;
    @Indexed
    private String userId;
    private String titulo;
    private String mensaje;
    private String tipo; // INFO, SUCCESS, WARNING, ALERT
    private String link;
    private boolean leida;
    private LocalDateTime fecha;

    public Notification() {
        this.fecha = LocalDateTime.now();
        this.leida = false;
    }

    public Notification(String userId, String titulo, String mensaje, String tipo, String link) {
        this();
        this.userId = userId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.link = link;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
