package com.innernest.decor.promo;

import java.math.BigDecimal;

public record PromoValidationResponse(String code, BigDecimal discount, String message) {
}
