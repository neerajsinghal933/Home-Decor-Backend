package com.innernest.decor.catalog;

public record ProductImageResponse(Long id, String url, String altText, int displayOrder, String color) {
  static ProductImageResponse from(ProductImage image) {
    return new ProductImageResponse(image.getId(), image.getUrl(), image.getAltText(), image.getDisplayOrder(), image.getColor());
  }
}
