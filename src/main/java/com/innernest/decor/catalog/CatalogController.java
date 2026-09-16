package com.innernest.decor.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final CatalogService service;

  CatalogController(CatalogService service) {
    this.service = service;
  }

  @GetMapping("/categories")
  List<CategoryResponse> categories() {
    return service.categories();
  }

  @GetMapping("/products")
  ProductListResponse products(@RequestParam(required = false) String search,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false) String tag,
                               @RequestParam(required = false) String color,
                               @RequestParam(required = false) String material,
                               @RequestParam(required = false) java.math.BigDecimal maxPrice,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "24") int size) {
    return service.products(search, category, sort, tag, color, material, maxPrice, page, size);
  }

  @GetMapping("/tags")
  List<TagResponse> tags() { return service.tags(); }

  @GetMapping("/products/{id}")
  ProductResponse product(@PathVariable Long id) {
    return service.product(id);
  }

  @GetMapping("/products/slug/{slug}")
  ProductResponse productBySlug(@PathVariable String slug) {
    return service.productBySlug(slug);
  }
}
