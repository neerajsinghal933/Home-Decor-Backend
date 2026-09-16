package com.innernest.decor.cart;

import com.innernest.decor.catalog.Product;
import java.math.BigDecimal;

public record CartItemResponse(
    Long id,
    Long productId,
    String name,
    String type,
    BigDecimal price,
    BigDecimal old,
    int reviews,
    String badge,
    String color,
    int qty,
    String size,
    String image,
    String img) {
  static CartItemResponse from(CartItem item) {
    Product product = item.getProduct();
    return new CartItemResponse(
        item.getId(),
        product.getId(),
        product.getName(),
        product.getCategory().getName(),
        product.getPrice(),
        product.getCompareAtPrice(),
        product.getReviewCount(),
        product.getBadge(),
        item.getColor(),
        item.getQty(),
        item.getSize(),
        product.getPrimaryImage(),
        product.getPrimaryImage());
  }
}
