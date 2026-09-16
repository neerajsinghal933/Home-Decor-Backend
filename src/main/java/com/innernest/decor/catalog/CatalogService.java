package com.innernest.decor.catalog;

import com.innernest.decor.common.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CatalogService {
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final TagRepository tags;

  CatalogService(CategoryRepository categories, ProductRepository products, TagRepository tags) {
    this.categories = categories;
    this.products = products;
    this.tags = tags;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> categories() {
    return categories.findByActiveTrueOrderByDisplayOrderAsc().stream().map(CategoryResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public ProductListResponse products(String search, String category, String sort, String tag, String color, String material, BigDecimal maxPrice, int page, int size) {
    String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
    String normalizedCategory = StringUtils.hasText(category) && !"All".equals(category) ? category.trim() : null;
    Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    if (normalizedSearch != null) spec = spec.and((root, query, cb) -> cb.or(
        cb.like(cb.lower(root.get("name")), "%" + normalizedSearch.toLowerCase() + "%"),
        cb.like(cb.lower(root.get("sku")), "%" + normalizedSearch.toLowerCase() + "%"),
        cb.like(cb.lower(root.get("description")), "%" + normalizedSearch.toLowerCase() + "%")));
    if (normalizedCategory != null) spec = spec.and((root, query, cb) -> cb.equal(root.join("category").get("name"), normalizedCategory));
    if (StringUtils.hasText(tag)) spec = spec.and((root, query, cb) -> cb.equal(root.join("tags").get("id"), Long.valueOf(tag)));
    if (StringUtils.hasText(color)) spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("color")), color.trim().toLowerCase()));
    if (StringUtils.hasText(material)) spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("material")), material.trim().toLowerCase()));
    if (maxPrice != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
    int safeSize = Math.min(Math.max(size, 1), 100);
    var rows = products.findAll(spec, PageRequest.of(Math.max(page, 0), safeSize, sort(sort))).map(ProductResponse::from);
    return new ProductListResponse(rows.getContent(), rows.getTotalElements(), rows.getNumber(), rows.getSize(), rows.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<TagResponse> tags() { return tags.findByActiveTrueOrderByNameAsc().stream().map(tag -> TagResponse.from(tag, 0)).toList(); }

  @Transactional(readOnly = true)
  public ProductResponse product(Long id) {
    Product product = products.findWithImagesById(id)
        .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    return ProductResponse.from(product);
  }

  @Transactional(readOnly = true)
  public ProductResponse productBySlug(String slug) {
    Product product = products.findWithImagesBySlugAndStatus(slug, ProductStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    return ProductResponse.from(product);
  }

  private Sort sort(String sort) {
    return switch (sort == null ? "" : sort) {
      case "price_asc" -> Sort.by("price").ascending();
      case "price_desc" -> Sort.by("price").descending();
      case "newest" -> Sort.by("createdAt").descending();
      case "best_selling" -> Sort.by("badge").descending();
      default -> Sort.by("displayOrder").ascending().and(Sort.by("id").ascending());
    };
  }
}
