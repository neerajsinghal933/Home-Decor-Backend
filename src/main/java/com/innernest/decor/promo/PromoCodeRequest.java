package com.innernest.decor.promo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PromoCodeRequest(
    @NotBlank @Size(max = 80) String code,
    @Size(max = 255) String description,
    @NotNull DiscountType discountType,
    @NotNull @DecimalMin("0.01") BigDecimal discountValue,
    @NotNull @DecimalMin("0.0") BigDecimal minimumOrderAmount,
    Boolean active) {
}
