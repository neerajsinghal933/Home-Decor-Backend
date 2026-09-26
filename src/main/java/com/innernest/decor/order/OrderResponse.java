package com.innernest.decor.order;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
    String id,
    Instant createdAt,
    OrderCustomerResponse customer,
    String paymentStatus,
    String paymentMethod,
    String deliveryMethod,
    String estimatedDelivery,
    String status,
    List<OrderItemResponse> items,
    OrderTotalsResponse totals) {
  public static OrderResponse from(Order order) {
    int count = order.getItems().stream().mapToInt(OrderItem::getQty).sum();
    return new OrderResponse(
        order.getOrderNumber(),
        order.getCreatedAt(),
        new OrderCustomerResponse(order.getCustomerName(), order.getCustomerPhone(), order.getCustomerEmail(), order.getAddress(), order.getCity(), order.getState(), order.getPincode(), order.getLandmark()),
        paymentLabel(order.getPaymentStatus()),
        order.getPaymentMethod(),
        order.getDeliveryMethod(),
        order.getEstimatedDelivery(),
        order.getStatus().name(),
        order.getItems().stream().map(OrderItemResponse::from).toList(),
        new OrderTotalsResponse(count, order.getSubtotal(), order.getShipping(), order.getEstimatedTax(), order.getDiscountAmount(), order.getTotal()));
  }

  private static String title(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase();
  }

  private static String paymentLabel(PaymentStatus status) {
    return switch (status) {
      case PAID -> "Paid";
      case PENDING -> "Pending";
      case FAILED -> "Failed";
      case REFUND_PENDING -> "Refund initiated";
      case PARTIALLY_REFUNDED -> "Partially refunded";
      case REFUND_FAILED -> "Refund needs attention";
      case REFUNDED -> "Refund completed";
    };
  }
}
