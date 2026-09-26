package com.innernest.decor.admin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StoredContent;
import com.innernest.decor.storage.StoredObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AdminUploadControllerTest {
  @Test
  void storesValidatedProductImageUnderUniqueKey() throws Exception {
    RecordingStorage storage = new RecordingStorage();
    AdminUploadController controller = new AdminUploadController(storage);
    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};

    UploadedImageResponse response = controller.productImage(
        new MockMultipartFile("file", "lamp.png", "image/png", png));

    assertTrue(storage.key.matches("product-images/[0-9a-f-]{36}\\.png"));
    assertEquals("image/png", storage.contentType);
    assertArrayEquals(png, storage.bytes);
    assertEquals("/uploads/" + storage.key, response.url());
    assertEquals(storage.key.substring("product-images/".length()), response.filename());
  }

  @Test
  void rejectsSpoofedImageBeforeStorage() {
    RecordingStorage storage = new RecordingStorage();
    AdminUploadController controller = new AdminUploadController(storage);

    assertThrows(BusinessRuleException.class, () -> controller.productImage(
        new MockMultipartFile("file", "not-really.png", "image/png", "not an image".getBytes())));
    assertEquals(null, storage.key);
  }

  private static class RecordingStorage implements StorageService {
    private String key;
    private String contentType;
    private byte[] bytes;

    @Override
    public StoredObject store(String key, String contentType, long contentLength, InputStream content) {
      this.key = key;
      this.contentType = contentType;
      try {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        content.transferTo(output);
        bytes = output.toByteArray();
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
      return new StoredObject(key, "/uploads/" + key, contentType);
    }

    @Override
    public StoredContent load(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String key) {
    }
  }
}
