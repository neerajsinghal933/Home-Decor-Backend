package com.innernest.decor.order;

import java.time.Instant;

public record OrderHistoryResponse(String previousStatus, String status, String actor, String note, Instant at) {
  static OrderHistoryResponse from(OrderStatusHistory history) {
    return new OrderHistoryResponse(history.getPreviousStatus() == null ? null : history.getPreviousStatus().name(),
        history.getNewStatus().name(), history.getActorType().name(), history.getNote(), history.getCreatedAt());
  }
}
