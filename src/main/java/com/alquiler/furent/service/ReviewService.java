package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.ReviewCreatedEvent;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.ReviewRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;

    public ReviewService(ReviewRepository reviewRepository, EventPublisher eventPublisher, MetricsConfig metricsConfig) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
    }

    @Cacheable(value = "reviews", key = "#productId")
    public List<Review> getReviewsByProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @CacheEvict(value = "reviews", key = "#review.productId")
    public Review saveReview(Review review) {
        Review saved = reviewRepository.save(review);
        metricsConfig.getReviewsCreated().increment();
        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        eventPublisher.publish(new ReviewCreatedEvent(this, saved, tenantId));
        return saved;
    }
}
