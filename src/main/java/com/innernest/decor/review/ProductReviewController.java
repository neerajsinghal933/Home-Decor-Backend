package com.innernest.decor.review;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {
  private final ProductReviewService service;

  ProductReviewController(ProductReviewService service) {
    this.service = service;
  }

  @GetMapping
  ProductReviewListResponse reviews(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "recent") String sort,
                                    @RequestParam(required = false) Integer rating,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
    return service.list(productId, sort, rating, page, size);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ProductReviewResponse create(@PathVariable Long productId,
                               @RequestParam int rating,
                               @RequestParam String title,
                               @RequestParam String body,
                               @RequestPart(required = false) MultipartFile image) throws IOException {
    return service.create(productId, rating, title, body, image);
  }
}
