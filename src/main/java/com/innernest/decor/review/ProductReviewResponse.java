package com.innernest.decor.review;

import java.time.Instant;

public record ProductReviewResponse(
    Long id,
    int rating,
    String title,
    String body,
    String imageUrl,
    String reviewerName,
    boolean verifiedPurchase,
    Instant createdAt) {
  static ProductReviewResponse from(ProductReview review) {
    return new ProductReviewResponse(
        review.getId(),
        review.getRating(),
        review.getTitle(),
        review.getBody(),
        review.getImageUrl(),
        review.getUser().getName(),
        review.isVerifiedPurchase(),
        review.getCreatedAt());
  }
}
