package com.alquiler.furent.repository;

import com.alquiler.furent.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByFechaDesc(String userId);
    List<Notification> findByUserIdAndLeidaFalseOrderByFechaDesc(String userId);
    long countByUserIdAndLeidaFalse(String userId);
    List<Notification> findTop10ByUserIdOrderByFechaDesc(String userId);
}
