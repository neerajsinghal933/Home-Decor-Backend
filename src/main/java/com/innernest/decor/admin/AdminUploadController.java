package com.innernest.decor.admin;

import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.storage.ImageUploadValidator;
import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StoredObject;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {
  private static final String PRODUCT_IMAGE_PREFIX = "product-images/";
  private final StorageService storage;

  AdminUploadController(StorageService storage) {
    this.storage = storage;
  }

  @PostMapping(value = "/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  UploadedImageResponse productImage(@RequestPart("file") MultipartFile file) throws IOException {
    ImageUploadValidator.ValidatedImage image = ImageUploadValidator.validate(file, "Image file is required");
    String filename = UUID.randomUUID() + "." + image.extension();
    StoredObject stored;
    try (var input = file.getInputStream()) {
      stored = storage.store(PRODUCT_IMAGE_PREFIX + filename, image.contentType(), file.getSize(), input);
    }
    return new UploadedImageResponse(filename, stored.url());
  }

  @DeleteMapping("/product-images/{filename:.+}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteProductImage(@PathVariable String filename) {
    if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp|gif)")) {
      throw new BusinessRuleException("Invalid product image filename");
    }
    storage.delete(PRODUCT_IMAGE_PREFIX + filename);
  }
}
