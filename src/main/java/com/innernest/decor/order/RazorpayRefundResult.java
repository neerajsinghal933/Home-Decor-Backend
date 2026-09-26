package com.innernest.decor.order;

public record RazorpayRefundResult(String id, String paymentId, long amount, String currency, String status,
                                   String reference) {
}
