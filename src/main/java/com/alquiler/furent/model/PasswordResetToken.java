package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "password_reset_tokens")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_user", def = "{'tenantId': 1, 'userId': 1}"),
    @CompoundIndex(name = "idx_token", def = "{'token': 1}", unique = true)
})
public class PasswordResetToken {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;

    public PasswordResetToken() {
        this.token = UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusHours(1);
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public PasswordResetToken(String userId) {
        this();
        this.userId = userId;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
