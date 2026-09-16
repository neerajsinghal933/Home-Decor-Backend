package com.innernest.decor.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RazorpayVerifyRequest(
    @NotBlank @Size(max = 80) String localOrderNumber,
    @NotBlank @Size(max = 120) String razorpayOrderId,
    @NotBlank @Size(max = 120) String razorpayPaymentId,
    @NotBlank @Size(max = 255) String razorpaySignature) {
}
