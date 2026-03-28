package com.alquiler.furent.repository;

import com.alquiler.furent.model.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {
    List<ContactMessage> findByLeido(boolean leido);
    long countByLeido(boolean leido);
    List<ContactMessage> findByLeidoFalseOrderByFechaCreacionDesc();
    long countByLeidoFalse();
    List<ContactMessage> findAllByOrderByFechaCreacionDesc();
    Page<ContactMessage> findByLeido(boolean leido, Pageable pageable);
}
