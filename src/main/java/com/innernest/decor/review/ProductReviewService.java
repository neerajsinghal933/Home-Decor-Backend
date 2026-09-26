package com.innernest.decor.review;

import com.innernest.decor.catalog.Product;
import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.catalog.ProductStatus;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.DuplicateResourceException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.order.OrderItemRepository;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.storage.ImageUploadValidator;
import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StoredObject;
import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductReviewService {
  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final String IMAGE_PREFIX = "review-images/";

  private final ProductReviewRepository reviews;
  private final ProductRepository products;
  private final UserRepository users;
  private final OrderItemRepository orderItems;
  private final StorageService storage;

  ProductReviewService(ProductReviewRepository reviews, ProductRepository products, UserRepository users,
                       OrderItemRepository orderItems, StorageService storage) {
    this.reviews = reviews;
    this.products = products;
    this.users = users;
    this.orderItems = orderItems;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public ProductReviewListResponse list(Long productId, String sort, Integer rating, int page, int size) {
    requireActiveProduct(productId);
    if (rating != null && (rating < 1 || rating > 5)) throw new BusinessRuleException("Rating filter must be between 1 and 5");
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);
    PageRequest request = PageRequest.of(safePage, safeSize, reviewSort(sort));
    var rows = rating == null
        ? reviews.findByProductId(productId, request)
        : reviews.findByProductIdAndRating(productId, rating, request);
    long total = reviews.countByProductId(productId);
    BigDecimal average = average(productId);
    Map<Integer, Long> counts = new HashMap<>();
    reviews.ratingCounts(productId).forEach(row -> counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));
    List<RatingBreakdownResponse> breakdown = java.util.stream.IntStream.iterate(5, value -> value >= 1, value -> value - 1)
        .mapToObj(value -> {
          long count = counts.getOrDefault(value, 0L);
          int percentage = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
          return new RatingBreakdownResponse(value, count, percentage);
        })
        .toList();
    boolean hasReviewed = SecuritySupport.currentUser()
        .map(principal -> reviews.existsByProductIdAndUserId(productId, principal.id()))
        .orElse(false);
    return new ProductReviewListResponse(
        average,
        total,
        breakdown,
        rows.getContent().stream().map(ProductReviewResponse::from).toList(),
        rows.getNumber(),
        rows.getTotalPages(),
        hasReviewed);
  }

  @Transactional
  public ProductReviewResponse create(Long productId, int rating, String title, String body, MultipartFile image) throws IOException {
    Long userId = SecuritySupport.currentUser()
        .orElseThrow(() -> new AccessDeniedException("Unauthorized"))
        .id();
    if (reviews.existsByProductIdAndUserId(productId, userId)) {
      throw new DuplicateResourceException("You have already reviewed this product");
    }
    if (rating < 1 || rating > 5) throw new BusinessRuleException("Choose a rating between 1 and 5");
    String cleanTitle = clean(title);
    String cleanBody = clean(body);
    if (cleanTitle == null || cleanTitle.length() > 140) throw new BusinessRuleException("Review title is required and must be 140 characters or less");
    if (cleanBody == null || cleanBody.length() > 4000) throw new BusinessRuleException("Review must be between 1 and 4000 characters");

    Product product = requireActiveProduct(productId);
    User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    ProductReview review = new ProductReview();
    review.setProduct(product);
    review.setUser(user);
    review.setRating(rating);
    review.setTitle(cleanTitle);
    review.setBody(cleanBody);
    review.setVerifiedPurchase(orderItems.hasPurchased(productId, userId));
    if (image != null && !image.isEmpty()) review.setImageUrl(storeImage(image));
    ProductReview saved = reviews.save(review);
    reviews.flush();
    updateProductAggregate(product);
    return ProductReviewResponse.from(saved);
  }

  private Product requireActiveProduct(Long productId) {
    return products.findById(productId)
        .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
  }

  private Sort reviewSort(String value) {
    String normalized = value == null ? "recent" : value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "highest" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
      case "lowest" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
      case "recent", "" -> Sort.by(Sort.Order.desc("createdAt"));
      default -> throw new BusinessRuleException("Invalid review sort");
    };
  }

  private String storeImage(MultipartFile file) throws IOException {
    if (file.getSize() > MAX_IMAGE_BYTES) throw new BusinessRuleException("Review image must be 5 MB or smaller");
    ImageUploadValidator.ValidatedImage image = ImageUploadValidator.validate(file, "Review image is empty");
    String filename = UUID.randomUUID() + "." + image.extension();
    StoredObject stored;
    try (var input = file.getInputStream()) {
      stored = storage.store(IMAGE_PREFIX + filename, image.contentType(), file.getSize(), input);
    }
    return stored.url();
  }

  private void updateProductAggregate(Product product) {
    long count = reviews.countByProductId(product.getId());
    product.setReviewCount(Math.toIntExact(count));
    product.setRating(average(product.getId()));
  }

  private BigDecimal average(Long productId) {
    Double value = reviews.averageRating(productId);
    return BigDecimal.valueOf(value == null ? 0 : value).setScale(1, RoundingMode.HALF_UP);
  }

  private String clean(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
