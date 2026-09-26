package com.innernest.decor.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.innernest.decor.common.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageServiceTest {
  @TempDir
  Path tempDir;

  @Test
  void storesLoadsAndDeletesWithinConfiguredRoot() throws Exception {
    LocalStorageService storage = new LocalStorageService(tempDir.toString(), new StorageUrlResolver("http://localhost"));
    byte[] bytes = new byte[] {1, 2, 3};

    storage.store("product-images/id.png", "image/png", bytes.length, new ByteArrayInputStream(bytes));
    try (StoredContent stored = storage.load("product-images/id.png")) {
      assertArrayEquals(bytes, stored.inputStream().readAllBytes());
    }
    storage.delete("product-images/id.png");

    assertThrows(ResourceNotFoundException.class, () -> storage.load("product-images/id.png"));
    assertThrows(IllegalArgumentException.class,
        () -> storage.store("../escape.png", "image/png", 0, new ByteArrayInputStream(new byte[0])));
  }
}
