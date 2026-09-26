package com.innernest.decor.order;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderRequestResponse(Long id, String type, String status, String orderNumber,
    String customerName, String customerEmail, Instant orderDate, BigDecimal orderAmount,
    String paymentStatus, String orderStatus, String razorpayPaymentId, String reason, String details,
    String adminNote, Instant requestedAt, RefundResponse refund) {
}
