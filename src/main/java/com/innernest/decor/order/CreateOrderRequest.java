package com.innernest.decor.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Size(max = 40) String phone,
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Size(max = 300) String address,
    @NotBlank @Size(max = 120) String city,
    @NotBlank @Size(max = 120) String state,
    @NotBlank @Size(max = 20) String pincode,
    @Size(max = 180) String landmark,
    @Size(max = 80) String paymentMethod,
    @Size(max = 80) String deliveryMethod,
    @Size(max = 80) String promoCode) {
}
