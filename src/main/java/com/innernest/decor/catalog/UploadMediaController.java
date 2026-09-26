package com.innernest.decor.catalog;

import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StoredContent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class UploadMediaController {
  private final StorageService storage;

  UploadMediaController(StorageService storage) {
    this.storage = storage;
  }

  @GetMapping("/uploads/product-images/{filename:.+}")
  ResponseEntity<StreamingResponseBody> productImage(@PathVariable String filename) {
    return image("product-images/" + filename);
  }

  @GetMapping("/uploads/profile-images/{filename:.+}")
  ResponseEntity<StreamingResponseBody> profileImage(@PathVariable String filename) {
    return image("profile-images/" + filename);
  }

  @GetMapping("/uploads/review-images/{filename:.+}")
  ResponseEntity<StreamingResponseBody> reviewImage(@PathVariable String filename) {
    return image("review-images/" + filename);
  }

  private ResponseEntity<StreamingResponseBody> image(String key) {
    StoredContent stored = storage.load(key);
    StreamingResponseBody body = output -> {
      try (stored) {
        stored.inputStream().transferTo(output);
      }
    };
    ResponseEntity.BodyBuilder response = ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(stored.contentType()))
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "public, max-age=31536000, immutable");
    if (stored.contentLength() >= 0) response.contentLength(stored.contentLength());
    return response.body(body);
  }
}
