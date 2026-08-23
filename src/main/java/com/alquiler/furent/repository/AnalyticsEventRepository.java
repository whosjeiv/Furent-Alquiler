package com.alquiler.furent.repository;

import com.alquiler.furent.model.AnalyticsEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
    List<AnalyticsEvent> findByTenantId(String tenantId);
    List<AnalyticsEvent> findByTenantIdAndEventType(String tenantId, String eventType);
    List<AnalyticsEvent> findByTenantIdAndTimestampBetween(String tenantId, LocalDateTime start, LocalDateTime end);
    long countByTenantIdAndEventType(String tenantId, String eventType);
}
