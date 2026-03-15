package com.alquiler.furent.repository;

import com.alquiler.furent.model.PendingCardPayment;

import java.util.Optional;

public interface PendingCardPaymentRepository extends org.springframework.data.mongodb.repository.MongoRepository<PendingCardPayment, String> {

    Optional<PendingCardPayment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
