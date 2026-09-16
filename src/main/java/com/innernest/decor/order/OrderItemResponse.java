package com.innernest.decor.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, String name, String sku, String image, String img, String color, String size, int qty, BigDecimal price, BigDecimal lineTotal) {
  static OrderItemResponse from(OrderItem item) {
    Long productId = item.getProduct() == null ? null : item.getProduct().getId();
    return new OrderItemResponse(productId, item.getProductName(), item.getProductSku(), item.getProductImage(), item.getProductImage(), item.getColor(), item.getSize(), item.getQty(), item.getUnitPrice(), item.getLineTotal());
  }
}
