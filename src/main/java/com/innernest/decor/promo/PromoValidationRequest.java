package com.innernest.decor.promo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PromoValidationRequest(@NotBlank String code, @NotNull BigDecimal subtotal) {
}
