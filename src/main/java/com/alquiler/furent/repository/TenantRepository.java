package com.alquiler.furent.repository;

import com.alquiler.furent.model.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, String> {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findByDominio(String dominio);
    Optional<Tenant> findByAdminEmail(String adminEmail);
    boolean existsBySlug(String slug);
}
