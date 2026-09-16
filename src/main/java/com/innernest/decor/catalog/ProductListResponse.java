package com.innernest.decor.catalog;

import java.util.List;

public record ProductListResponse(List<ProductResponse> items, long total, int page, int size, int totalPages) {
  public ProductListResponse(List<ProductResponse> items, long total) { this(items, total, 0, items.size(), 1); }
}
