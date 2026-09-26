package com.innernest.decor.admin;

import com.innernest.decor.catalog.CategoryRepository;
import com.innernest.decor.catalog.Product;
import com.innernest.decor.catalog.ProductImageInput;
import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.catalog.ProductResponse;
import com.innernest.decor.catalog.ProductStatus;
import com.innernest.decor.catalog.ProductSizeVariantInput;
import com.innernest.decor.catalog.Tag;
import com.innernest.decor.catalog.TagRepository;
import com.innernest.decor.catalog.TagRequest;
import com.innernest.decor.catalog.TagResponse;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.order.Order;
import com.innernest.decor.order.OrderRepository;
import com.innernest.decor.order.OrderResponse;
import com.innernest.decor.order.OrderStatus;
import com.innernest.decor.order.OrderStatusHistory;
import com.innernest.decor.order.OrderStatusHistoryRepository;
import com.innernest.decor.order.OrderActorType;
import com.innernest.decor.user.UserRepository;
import com.innernest.decor.user.UserResponse;
import com.innernest.decor.content.NewsletterRepository;
import com.innernest.decor.content.NewsletterSubscriptionResponse;
import com.innernest.decor.storage.StorageCleanup;
import java.util.EnumMap;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final OrderRepository orders;
  private final UserRepository users;
  private final TagRepository tags;
  private final StorageCleanup storageCleanup;
  private final JdbcTemplate jdbc;
  private final NewsletterRepository newsletters;
  private final OrderStatusHistoryRepository orderHistory;

  AdminService(ProductRepository products, CategoryRepository categories, OrderRepository orders, UserRepository users,
               TagRepository tags, StorageCleanup storageCleanup, JdbcTemplate jdbc, NewsletterRepository newsletters,
               OrderStatusHistoryRepository orderHistory) {
    this.products = products;
    this.categories = categories;
    this.orders = orders;
    this.users = users;
    this.tags = tags;
    this.storageCleanup = storageCleanup;
    this.jdbc = jdbc;
    this.newsletters = newsletters;
    this.orderHistory = orderHistory;
  }

  @Transactional(readOnly = true)
  public AdminDashboardResponse dashboard() {
    List<Product> productRows = products.findAll();
    List<Order> orderRows = orders.findAllByOrderByCreatedAtDesc();
    Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
    for (OrderStatus status : OrderStatus.values()) {
      byStatus.put(status.name(), orderRows.stream().filter(order -> order.getStatus() == status).count());
    }
    return new AdminDashboardResponse(
        productRows.size(),
        productRows.stream().filter(product -> product.getStatus() == ProductStatus.ACTIVE).count(),
        productRows.stream().filter(product -> product.getStock() > 0 && product.getStock() <= 5).count(),
        productRows.stream().filter(product -> product.getStock() == 0).count(),
        orderRows.size(),
        byStatus,
        users.count(),
        orderRows.stream().limit(6).map(OrderResponse::from).toList());
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> products() {
    return products.findAllByOrderByDisplayOrderAscIdAsc().stream().map(ProductResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public AdminProductListResponse products(String search, String category, Long tagId, BigDecimal minPrice, BigDecimal maxPrice, String stock, String status, String sort, int page, int size) {
    Specification<Product> spec = Specification.where(null);
    if (search != null && !search.isBlank()) { String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%"; spec = spec.and((root,q,cb) -> cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("sku")), term))); }
    if (category != null && !category.isBlank()) spec = spec.and((root,q,cb) -> cb.equal(root.join("category").get("slug"), category));
    if (tagId != null) spec = spec.and((root,q,cb) -> cb.equal(root.join("tags").get("id"), tagId));
    if (minPrice != null) spec = spec.and((root,q,cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    if (maxPrice != null) spec = spec.and((root,q,cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
    if ("in_stock".equals(stock)) spec = spec.and((root,q,cb) -> cb.greaterThan(root.get("stock"), 0));
    if ("low_stock".equals(stock)) spec = spec.and((root,q,cb) -> cb.and(cb.greaterThan(root.get("stock"), 0), cb.lessThanOrEqualTo(root.get("stock"), 5)));
    if ("out_of_stock".equals(stock)) spec = spec.and((root,q,cb) -> cb.equal(root.get("stock"), 0));
    if (status != null && !status.isBlank()) try { ProductStatus value = ProductStatus.valueOf(status.toUpperCase(Locale.ROOT)); spec = spec.and((root,q,cb) -> cb.equal(root.get("status"), value)); } catch (IllegalArgumentException e) { throw new BusinessRuleException("Invalid product status"); }
    int safeSize = Math.min(Math.max(size, 1), 100);
    Sort order = switch (sort == null ? "" : sort) { case "name" -> Sort.by("name"); case "price_asc" -> Sort.by("price"); case "price_desc" -> Sort.by("price").descending(); case "stock" -> Sort.by("stock"); case "created_at" -> Sort.by("createdAt").descending(); case "updated_at" -> Sort.by("updatedAt").descending(); default -> Sort.by("displayOrder").and(Sort.by("id")); };
    var rows = products.findAll(spec, PageRequest.of(Math.max(page, 0), safeSize, order));
    return new AdminProductListResponse(rows.getContent().stream().map(ProductResponse::from).toList(), rows.getTotalElements(), rows.getNumber(), rows.getSize(), rows.getTotalPages());
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<TagResponse> tags(String search, int page, int size) {
    return tags.search(search == null || search.isBlank() ? null : search.trim(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name")))
        .map(tag -> TagResponse.from(tag, tags.countProductsByTagId(tag.getId())));
  }

  @Transactional
  public TagResponse createTag(TagRequest request) { Tag tag = new Tag(); applyTag(tag, request); return TagResponse.from(tags.save(tag), 0); }
  @Transactional
  public TagResponse updateTag(Long id, TagRequest request) { Tag tag = tags.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag not found")); applyTag(tag, request); return TagResponse.from(tags.save(tag), tags.countProductsByTagId(id)); }
  @Transactional
  public void deleteTag(Long id) { if (tags.countProductsByTagId(id) > 0) throw new BusinessRuleException("This tag is assigned to products. Unassign it before deletion."); tags.delete(tags.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tag not found"))); }

  @Transactional(readOnly = true)
  public List<UserResponse> customers() {
    return users.findAll().stream()
        .sorted(java.util.Comparator.comparing(user -> user.getId(), java.util.Comparator.reverseOrder()))
        .map(UserResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<NewsletterSubscriptionResponse> subscribers() {
    return newsletters.findAllByOrderByCreatedAtDesc().stream().map(NewsletterSubscriptionResponse::from).toList();
  }

  @Transactional
  public ProductResponse createProduct(AdminProductRequest request) {
    Product product = new Product();
    applyProduct(product, request);
    return ProductResponse.from(products.save(product));
  }

  @Transactional
  public ProductResponse updateProduct(Long id, AdminProductRequest request) {
    Product product = products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    Set<String> oldImageUrls = productImageUrls(product);
    applyProduct(product, request);
    Product saved = products.save(product);
    Set<String> retainedImageUrls = productImageUrls(saved);
    oldImageUrls.stream()
        .filter(url -> !retainedImageUrls.contains(url))
        .forEach(url -> storageCleanup.deleteAfterCommit(url, "product-images/"));
    return ProductResponse.from(saved);
  }

  @Transactional
  public ProductResponse deactivateProduct(Long id) {
    Product product = products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    product.setStatus(ProductStatus.INACTIVE);
    return ProductResponse.from(products.save(product));
  }

  @Transactional
  public void permanentlyDeleteProduct(Long id) {
    Product product = products.findWithImagesById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    Set<String> imageUrls = productImageUrls(product);
    Set<String> orderHistoryImages = new java.util.HashSet<>(jdbc.queryForList(
        "select distinct product_image from order_items where product_id = ? and product_image is not null",
        String.class,
        id));

    // Carts are transient, while order item snapshots must remain available for order history.
    jdbc.update("delete from cart_items where product_id = ?", id);
    jdbc.update("update order_items set product_id = null where product_id = ?", id);
    jdbc.update("update razorpay_payment_attempt_items set product_id = null where product_id = ?", id);
    products.delete(product);
    products.flush();

    imageUrls.stream()
        .filter(url -> !orderHistoryImages.contains(url))
        .forEach(url -> storageCleanup.deleteAfterCommit(url, "product-images/"));
  }

  @Transactional
  public List<ProductResponse> updateProductOrder(List<ProductOrderRequest> request) {
    for (ProductOrderRequest row : request) {
      Product product = products.findById(row.id())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
      product.setDisplayOrder(row.displayOrder());
    }
    return products.findAllByOrderByDisplayOrderAscIdAsc().stream().map(ProductResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> orders(String status) {
    if (status == null || status.isBlank()) return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    OrderStatus parsed;
    try {
      parsed = OrderStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException("Invalid order status");
    }
    return orders.findByStatusOrderByCreatedAtDesc(parsed).stream().map(OrderResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public OrderResponse order(String orderNumber) {
    return orders.findWithItemsByOrderNumber(orderNumber).map(OrderResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
  }

  @Transactional
  public OrderResponse updateOrderStatus(String orderNumber, AdminOrderStatusRequest request) {
    Order order = orders.findWithItemsByOrderNumber(orderNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (!allowed(order.getStatus(), request.status())) {
      throw new BusinessRuleException("Order cannot move from " + order.getStatus() + " to " + request.status());
    }
    if (request.status() == OrderStatus.CANCELLED && order.getPaymentStatus() == com.innernest.decor.order.PaymentStatus.PAID) {
      throw new BusinessRuleException("Paid orders must be cancelled through the cancellation request workflow");
    }
    OrderStatus previous = order.getStatus();
    order.setStatus(request.status());
    if (previous == OrderStatus.RETURN_APPROVED && request.status() == OrderStatus.RETURNED) {
      order.getItems().forEach(item -> {
        if (item.getProduct() == null) return;
        products.findForInventoryUpdate(item.getProduct().getId())
            .ifPresent(product -> product.setStock(product.getStock() + item.getQty()));
      });
    }
    if (previous != request.status()) {
      orderHistory.save(new OrderStatusHistory(order, previous, request.status(), OrderActorType.ADMIN,
          "FULFILLMENT", null, "Fulfillment status updated"));
    }
    return OrderResponse.from(orders.save(order));
  }

  private void applyProduct(Product product, AdminProductRequest request) {
    product.setSku(request.sku().trim());
    product.setSlug(request.slug().trim());
    product.setName(request.name().trim());
    product.setDescription(blankToNull(request.description()));
    product.setCategory(categories.findBySlug(request.categoryId())
        .or(() -> parseId(request.categoryId()).flatMap(categories::findById))
        .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
    List<AdminProductSizeVariantRequest> requestedVariants = request.sizeVariants() == null
        ? List.of() : request.sizeVariants();
    Set<String> normalizedSizes = new java.util.HashSet<>();
    for (AdminProductSizeVariantRequest variant : requestedVariants) {
      String normalized = variant.size().trim().toLowerCase(Locale.ROOT);
      if (!normalizedSizes.add(normalized)) throw new BusinessRuleException("Each size must be unique");
    }
    product.replaceSizeVariants(requestedVariants.stream()
        .map(variant -> new ProductSizeVariantInput(variant.size(), variant.price()))
        .toList());
    product.setPrice(requestedVariants.isEmpty() ? request.price() : requestedVariants.get(0).price());
    product.setCompareAtPrice(request.old());
    product.setBadge(blankToNull(request.badge()));
    if (product.getId() == null) product.setRating(java.math.BigDecimal.ZERO);
    product.setColor(blankToNull(request.color()));
    product.setMaterial(blankToNull(request.material()));
    product.setDimensions(blankToNull(request.dimensions()));
    product.setStock(request.stock());
    if (product.getId() == null) product.setReviewCount(0);
    product.setFeatured(request.featured());
    product.setDisplayOrder(request.displayOrder());
    product.setStatus(request.active() == null || request.active() ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
    product.setPrimaryImage(blankToNull(request.image()));
    Set<Long> requestedTagIds = request.tagIds() == null ? Set.of() : new java.util.LinkedHashSet<>(request.tagIds());
    List<Tag> selectedTags = tags.findByIdIn(requestedTagIds);
    if (selectedTags.size() != requestedTagIds.size()) throw new BusinessRuleException("One or more selected tags do not exist");
    if (selectedTags.stream().anyMatch(tag -> !tag.isActive())) throw new BusinessRuleException("Inactive tags cannot be assigned to products");
    product.setTags(selectedTags);
    List<ProductImageInput> imageUrls = new java.util.ArrayList<>();
    if (request.image() != null && !request.image().isBlank()) imageUrls.add(new ProductImageInput(request.image().trim(), null));
    if (request.images() != null) {
      request.images().stream()
          .filter(value -> value != null && !value.isBlank())
          .map(String::trim)
          .filter(value -> imageUrls.stream().noneMatch(existing -> existing.url().equals(value)))
          .map(value -> new ProductImageInput(value, null))
          .forEach(imageUrls::add);
    }
    if (request.colorImages() != null) {
      request.colorImages().stream()
          .filter(row -> row != null && row.url() != null && !row.url().isBlank())
          .filter(row -> row.color() != null && !row.color().isBlank())
          .map(row -> new ProductImageInput(row.url().trim(), row.color().trim()))
          .forEach(imageUrls::add);
    }
    product.replaceImages(imageUrls);
  }

  private boolean allowed(OrderStatus from, OrderStatus to) {
    if (from == to) return true;
    if (from == OrderStatus.CANCELLED || from == OrderStatus.DELIVERED) return false;
    if (to == OrderStatus.CANCELLED) return true;
    return switch (from) {
      case PLACED -> to == OrderStatus.PROCESSING;
      case PROCESSING -> to == OrderStatus.SHIPPED;
      case SHIPPED -> to == OrderStatus.DELIVERED;
      case RETURN_APPROVED -> to == OrderStatus.RETURNED;
      default -> false;
    };
  }

  private java.util.Optional<Long> parseId(String value) {
    try {
      return java.util.Optional.of(Long.valueOf(value));
    } catch (NumberFormatException ex) {
      return java.util.Optional.empty();
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Set<String> productImageUrls(Product product) {
    Set<String> urls = new java.util.LinkedHashSet<>();
    if (product.getPrimaryImage() != null) urls.add(product.getPrimaryImage());
    product.getImages().stream().map(image -> image.getUrl()).forEach(urls::add);
    return urls;
  }

  private void applyTag(Tag tag, TagRequest request) {
    String name = request.name() == null ? "" : request.name().trim();
    tags.findByNameIgnoreCase(name).filter(existing -> !existing.getId().equals(tag.getId())).ifPresent(existing -> { throw new com.innernest.decor.common.DuplicateResourceException("A tag with that name already exists"); });
    tag.setName(name);
    if (request.active() != null) tag.setActive(request.active());
  }
}
