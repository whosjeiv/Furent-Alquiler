package com.alquiler.furent.repository;

import com.alquiler.furent.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByReservaId(String reservaId);

    List<Payment> findByUsuarioId(String usuarioId);
    
    Optional<Payment> findByReferencia(String referencia);
}
