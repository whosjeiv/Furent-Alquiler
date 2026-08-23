package com.alquiler.furent.repository;

import com.alquiler.furent.model.Combo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ComboRepository extends MongoRepository<Combo, String> {
    List<Combo> findByActivoTrue();
    List<Combo> findByTenantId(String tenantId);
}
