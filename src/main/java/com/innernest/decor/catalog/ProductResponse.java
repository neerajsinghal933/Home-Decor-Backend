package com.innernest.decor.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String sku,
    String slug,
    String name,
    String description,
    String type,
    String categoryId,
    BigDecimal price,
    BigDecimal old,
    BigDecimal rating,
    int reviews,
    String badge,
    String color,
    String material,
    String dimensions,
    int stock,
    boolean featured,
    int displayOrder,
    String status,
    String image,
    String img,
    List<ProductImageResponse> images,
    List<ProductSizeVariantResponse> sizeVariants,
    List<TagResponse> tags,
    Instant createdAt,
    Instant updatedAt) {
  public static ProductResponse from(Product product) {
    List<ProductImageResponse> images = product.getImagesInDisplayOrder().stream()
        .map(ProductImageResponse::from)
        .toList();
    return new ProductResponse(
        product.getId(),
        product.getSku(),
        product.getSlug(),
        product.getName(),
        product.getDescription(),
        product.getCategory().getName(),
        product.getCategory().getSlug(),
        product.getPrice(),
        product.getCompareAtPrice(),
        product.getRating(),
        product.getReviewCount(),
        product.getBadge(),
        product.getColor(),
        product.getMaterial(),
        product.getDimensions(),
        product.getStock(),
        product.isFeatured(),
        product.getDisplayOrder(),
        product.getStatus().name(),
        product.getPrimaryImage(),
        product.getPrimaryImage(),
        images,
        product.getSizeVariantsInDisplayOrder().stream().map(ProductSizeVariantResponse::from).toList(),
        product.getTags().stream().sorted(java.util.Comparator.comparing(Tag::getName)).map(tag -> TagResponse.from(tag, 0)).toList(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }
}
