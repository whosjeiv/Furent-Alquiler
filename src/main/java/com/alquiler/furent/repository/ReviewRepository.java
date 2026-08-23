package com.alquiler.furent.repository;

import com.alquiler.furent.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);
    List<Review> findAllByOrderByCreatedAtDesc();
    long countByAdminResponseIsNull();
}
