package com.innernest.decor.admin;

import com.innernest.decor.common.BusinessRuleException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {
  private static final Path PRODUCT_IMAGE_DIR = Path.of("uploads", "product-images");
  private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

  @PostMapping(value = "/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  UploadedImageResponse productImage(@RequestPart("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) throw new BusinessRuleException("Image file is required");
    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String extension = extension(original);
    if (!EXTENSIONS.contains(extension)) throw new BusinessRuleException("Only JPG, PNG, WEBP or GIF images are allowed");
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) throw new BusinessRuleException("Uploaded file must be an image");
    Files.createDirectories(PRODUCT_IMAGE_DIR);
    String filename = UUID.randomUUID() + "." + extension;
    Files.copy(file.getInputStream(), PRODUCT_IMAGE_DIR.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    return new UploadedImageResponse(filename, "http://localhost:8080/uploads/product-images/" + filename);
  }

  private String extension(String filename) {
    int index = filename.lastIndexOf('.');
    return index < 0 ? "" : filename.substring(index + 1).toLowerCase();
  }
}
