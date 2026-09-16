package com.innernest.decor.catalog;

import java.time.Instant;

public record TagResponse(Long id, String name, boolean active, long productCount, Instant createdAt, Instant updatedAt) {
  public static TagResponse from(Tag tag, long productCount) {
    return new TagResponse(tag.getId(), tag.getName(), tag.isActive(), productCount, tag.getCreatedAt(), tag.getUpdatedAt());
  }
}
