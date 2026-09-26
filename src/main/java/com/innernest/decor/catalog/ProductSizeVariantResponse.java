package com.innernest.decor.catalog;

import java.math.BigDecimal;

public record ProductSizeVariantResponse(Long id, String size, BigDecimal price, int displayOrder) {
  public static ProductSizeVariantResponse from(ProductSizeVariant variant) {
    return new ProductSizeVariantResponse(variant.getId(), variant.getSizeLabel(), variant.getPrice(), variant.getDisplayOrder());
  }
}
