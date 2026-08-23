package com.alquiler.furent.repository;

import com.alquiler.furent.model.StatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface StatusHistoryRepository extends MongoRepository<StatusHistory, String> {
    List<StatusHistory> findByReservaIdOrderByFechaAsc(String reservaId);
}
