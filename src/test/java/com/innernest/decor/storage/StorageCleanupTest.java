package com.innernest.decor.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StorageCleanupTest {
  @Test
  void extractsOnlyManagedKeysFromRelativeAndLegacyAbsoluteUrls() {
    assertEquals("product-images/id.png",
        StorageCleanup.keyFromUrl("/uploads/product-images/id.png", "product-images/").orElseThrow());
    assertEquals("product-images/id.png",
        StorageCleanup.keyFromUrl("http://localhost:8080/uploads/product-images/id.png", "product-images/").orElseThrow());
    assertTrue(StorageCleanup.keyFromUrl("https://cdn.example/image.png", "product-images/").isEmpty());
    assertTrue(StorageCleanup.keyFromUrl("/uploads/profile-images/id.png", "product-images/").isEmpty());
  }
}
