package com.innernest.decor.order;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(Long id, BigDecimal amount, String currency, String status, String reference,
                             Instant initiatedAt, Instant completedAt) {
  static RefundResponse from(RefundTransaction refund) {
    if (refund == null) return null;
    String safeReference = refund.getRefundReference() != null ? refund.getRefundReference() : refund.getRazorpayRefundId();
    return new RefundResponse(refund.getId(), refund.getAmount(), refund.getCurrency(), refund.getStatus().name(),
        safeReference, refund.getInitiatedAt(), refund.getCompletedAt());
  }
}
