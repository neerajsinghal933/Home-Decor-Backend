package com.innernest.decor.order;

import java.math.BigDecimal;

public record RazorpayCreateOrderResponse(
    String keyId,
    String razorpayOrderId,
    String localOrderNumber,
    BigDecimal amount,
    int amountPaise,
    String currency,
    String name,
    String email,
    String phone) {
}
