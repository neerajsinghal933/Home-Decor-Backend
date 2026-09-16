package com.innernest.decor.catalog;

import com.innernest.decor.common.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UploadMediaController {
  private static final Path PRODUCT_IMAGE_DIR = Path.of("uploads", "product-images");
  private static final Path PROFILE_IMAGE_DIR = Path.of("uploads", "profile-images");

  @GetMapping("/uploads/product-images/{filename:.+}")
  ResponseEntity<ByteArrayResource> productImage(@PathVariable String filename) throws IOException {
    return image(PRODUCT_IMAGE_DIR, filename);
  }

  @GetMapping("/uploads/profile-images/{filename:.+}")
  ResponseEntity<ByteArrayResource> profileImage(@PathVariable String filename) throws IOException {
    return image(PROFILE_IMAGE_DIR, filename);
  }

  private ResponseEntity<ByteArrayResource> image(Path dir, String filename) throws IOException {
    Path file = dir.resolve(filename).normalize();
    if (!file.startsWith(dir) || !Files.exists(file)) throw new ResourceNotFoundException("Image not found");
    String contentType = Files.probeContentType(file);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
        .body(new ByteArrayResource(Files.readAllBytes(file)));
  }
}
