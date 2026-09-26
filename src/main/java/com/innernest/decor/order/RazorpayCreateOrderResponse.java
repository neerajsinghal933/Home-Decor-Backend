package com.innernest.decor.order;

public record RazorpayCreateOrderResponse(
    String keyId,
    String razorpayOrderId,
    String receipt,
    long amountPaise,
    String currency,
    String name,
    String email,
    String phone) {
}
