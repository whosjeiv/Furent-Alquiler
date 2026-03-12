package com.alquiler.furent.repository;

import com.alquiler.furent.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SessionRepository extends MongoRepository<Session, String> {
    List<Session> findByUserIdAndActiveTrue(String userId);
    List<Session> findByTenantId(String tenantId);
    void deleteByUserId(String userId);
}
