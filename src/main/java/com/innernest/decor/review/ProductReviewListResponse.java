package com.innernest.decor.review;

import java.math.BigDecimal;
import java.util.List;

public record ProductReviewListResponse(
    BigDecimal averageRating,
    long totalReviews,
    List<RatingBreakdownResponse> breakdown,
    List<ProductReviewResponse> reviews,
    int page,
    int totalPages,
    boolean hasReviewed) {
}
