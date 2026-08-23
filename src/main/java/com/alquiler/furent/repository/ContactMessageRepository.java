package com.alquiler.furent.repository;

import com.alquiler.furent.model.ContactMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {
    List<ContactMessage> findByLeidoFalseOrderByFechaCreacionDesc();
    long countByLeidoFalse();
    List<ContactMessage> findAllByOrderByFechaCreacionDesc();
}
