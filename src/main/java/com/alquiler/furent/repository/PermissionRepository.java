package com.alquiler.furent.repository;

import com.alquiler.furent.model.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface PermissionRepository extends MongoRepository<Permission, String> {
    Optional<Permission> findByRoleName(String roleName);
    List<Permission> findByTenantId(String tenantId);
    Optional<Permission> findByRoleNameAndTenantId(String roleName, String tenantId);
}
