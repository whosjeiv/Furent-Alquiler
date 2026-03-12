package com.alquiler.furent.event;

import com.alquiler.furent.model.Review;

public class ReviewCreatedEvent extends FurentEvent {

    private final Review review;

    public ReviewCreatedEvent(Object source, Review review, String tenantId) {
        super(source, tenantId, review.getUserId());
        this.review = review;
    }

    public Review getReview() {
        return review;
    }
}
