package com.alquiler.furent.service;

import com.alquiler.furent.config.TenantContext;
import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.event.ReviewCreatedEvent;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReviewRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final MetricsConfig metricsConfig;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         EventPublisher eventPublisher, MetricsConfig metricsConfig) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
        this.metricsConfig = metricsConfig;
    }

    @Cacheable(value = "reviews", key = "#productId")
    public List<Review> getReviewsByProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countUnanswered() {
        return reviewRepository.countByAdminResponseIsNull();
    }

    @Caching(evict = {
        @CacheEvict(value = "reviews", key = "#review.productId"),
        @CacheEvict(value = "product-detail", key = "#review.productId"),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "featured-products", allEntries = true)
    })
    public Review saveReview(Review review) {
        Review saved = reviewRepository.save(review);
        metricsConfig.getReviewsCreated().increment();

        // Recalculate and update product rating/review count from real reviews
        updateProductRatingStats(review.getProductId());

        String tenantId = TenantContext.getCurrentTenant() != null ? TenantContext.getCurrentTenant() : "default";
        eventPublisher.publish(new ReviewCreatedEvent(this, saved, tenantId));
        return saved;
    }

    @Caching(evict = {
        @CacheEvict(value = "reviews", allEntries = true),
        @CacheEvict(value = "product-detail", allEntries = true),
        @CacheEvict(value = "products", allEntries = true),
        @CacheEvict(value = "featured-products", allEntries = true)
    })
    public void deleteReview(String id) {
        reviewRepository.findById(id).ifPresent(review -> {
            String productId = review.getProductId();
            reviewRepository.deleteById(id);
            updateProductRatingStats(productId);
        });
    }

    public void respondToReview(String id, String response) {
        reviewRepository.findById(id).ifPresent(review -> {
            review.setAdminResponse(response);
            review.setRespondedAt(LocalDateTime.now());
            reviewRepository.save(review);
        });
    }

    private void updateProductRatingStats(String productId) {
        List<Review> allReviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        productRepository.findById(productId).ifPresent(product -> {
            if (allReviews.isEmpty()) {
                product.setCalificacion(0);
                product.setCantidadResenas(0);
            } else {
                double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                // Round to 1 decimal place
                product.setCalificacion(Math.round(avg * 10.0) / 10.0);
                product.setCantidadResenas(allReviews.size());
            }
            productRepository.save(product);
        });
    }
}
