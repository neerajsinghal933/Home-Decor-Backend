package com.innernest.decor.promo;

import java.math.BigDecimal;

public record PromoCodeResponse(
    Long id,
    String code,
    String description,
    DiscountType discountType,
    BigDecimal discountValue,
    BigDecimal minimumOrderAmount,
    boolean active) {
  static PromoCodeResponse from(PromoCode promo) {
    return new PromoCodeResponse(
        promo.getId(),
        promo.getCode(),
        promo.getDescription(),
        promo.getDiscountType(),
        promo.getDiscountValue(),
        promo.getMinimumOrderAmount(),
        promo.isActive());
  }
}
