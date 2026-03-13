package com.alquiler.furent.repository;

import com.alquiler.furent.model.WorkerPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkerPaymentRepository extends MongoRepository<WorkerPayment, String> {

    List<WorkerPayment> findByTrabajadorEmail(String trabajadorEmail);
}

