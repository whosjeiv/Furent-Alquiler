package com.alquiler.furent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "report_cache")
public class ReportCache {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String reportType;

    private Map<String, Object> data;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;

    public ReportCache() {
        this.generatedAt = LocalDateTime.now();
    }

    public ReportCache(String tenantId, String reportType, Map<String, Object> data, LocalDateTime expiresAt) {
        this();
        this.tenantId = tenantId;
        this.reportType = reportType;
        this.data = data;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
