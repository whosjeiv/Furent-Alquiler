package com.alquiler.furent.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "contact_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_leido", def = "{'tenantId': 1, 'leido': 1}"),
    @CompoundIndex(name = "idx_fecha", def = "{'fechaCreacion': -1}")
})
public class ContactMessage {

    @Id
    private String id;

    @Indexed
    private String tenantId;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;
    
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String telefono;
    
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String asunto;
    
    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "Máximo 2000 caracteres")
    private String mensaje;
    
    private boolean leido;
    
    @Indexed
    private LocalDateTime fechaCreacion;

    public ContactMessage() {
        this.fechaCreacion = LocalDateTime.now();
        this.leido = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
