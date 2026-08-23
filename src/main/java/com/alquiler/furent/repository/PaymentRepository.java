package com.alquiler.furent.repository;

import com.alquiler.furent.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByReservaId(String reservaId);

    List<Payment> findByUsuarioId(String usuarioId);
}
