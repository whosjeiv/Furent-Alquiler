package com.alquiler.furent.service;

import com.alquiler.furent.config.MetricsConfig;
import com.alquiler.furent.event.EventPublisher;
import com.alquiler.furent.model.Review;
import com.alquiler.furent.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private MetricsConfig metricsConfig;
    @InjectMocks private ReviewService reviewService;

    @Test
    void getReviewsByProduct_shouldDelegateToRepository() {
        Review r1 = new Review("prod-1", "user-1", "Juan", 5, "Excelente");
        Review r2 = new Review("prod-1", "user-2", "Ana", 4, "Muy bueno");
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc("prod-1"))
                .thenReturn(List.of(r1, r2));

        List<Review> reviews = reviewService.getReviewsByProduct("prod-1");

        assertEquals(2, reviews.size());
        assertEquals("Excelente", reviews.get(0).getComment());
        verify(reviewRepository).findByProductIdOrderByCreatedAtDesc("prod-1");
    }

    @Test
    void getReviewsByProduct_noReviews_returnsEmptyList() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc("prod-999"))
                .thenReturn(List.of());

        List<Review> reviews = reviewService.getReviewsByProduct("prod-999");

        assertTrue(reviews.isEmpty());
    }

    @Test
    void saveReview_shouldSaveAndPublishEvent() {
        Review review = new Review("prod-1", "user-1", "Juan", 5, "Excelente");
        Review saved = new Review("prod-1", "user-1", "Juan", 5, "Excelente");
        saved.setId("review-001");

        when(reviewRepository.save(review)).thenReturn(saved);

        Review result = reviewService.saveReview(review);

        assertEquals("review-001", result.getId());
        verify(reviewRepository).save(review);
        verify(eventPublisher).publish(any());
    }
}
