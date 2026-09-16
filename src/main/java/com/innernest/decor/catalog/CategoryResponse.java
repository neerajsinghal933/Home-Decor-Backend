package com.innernest.decor.catalog;

public record CategoryResponse(String id, String name, String image) {
  static CategoryResponse from(Category category) {
    return new CategoryResponse(category.getSlug(), category.getName(), category.getImage());
  }
}
