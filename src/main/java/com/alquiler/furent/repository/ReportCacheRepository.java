package com.alquiler.furent.repository;

import com.alquiler.furent.model.ReportCache;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReportCacheRepository extends MongoRepository<ReportCache, String> {

    Optional<ReportCache> findByTenantIdAndReportType(String tenantId, String reportType);

    void deleteByTenantIdAndReportType(String tenantId, String reportType);
}
