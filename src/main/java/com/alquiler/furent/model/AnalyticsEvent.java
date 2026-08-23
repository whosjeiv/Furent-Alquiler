package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Document(collection = "analytics_events")
public class AnalyticsEvent {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String eventType;
    private String userId;
    private String entityType;
    private String entityId;
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime timestamp;

    public AnalyticsEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public AnalyticsEvent(String tenantId, String eventType, String userId, String entityType, String entityId) {
        this();
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
