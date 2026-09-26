package com.innernest.decor.order;

import java.time.Instant;

public record OrderRequestResponse(Long id, String type, String status, String reason, String details,
                                   String adminNote, Instant requestedAt, Instant reviewedAt) {
  static OrderRequestResponse from(OrderServiceRequest request) {
    if (request == null) return null;
    return new OrderRequestResponse(request.getId(), request.getType().name(), request.getStatus().name(),
        request.getReason(), request.getDetails(), request.getAdminNote(), request.getRequestedAt(), request.getReviewedAt());
  }
}
