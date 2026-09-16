package com.innernest.decor.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String sku;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private String name;

  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(name = "compare_at_price")
  private BigDecimal compareAtPrice;

  private String badge;
  private String color;
  private String material;
  private String dimensions;
  private int stock;

  @Column(name = "review_count")
  private int reviewCount;

  @Column(nullable = false)
  private BigDecimal rating = BigDecimal.valueOf(4.8);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus status = ProductStatus.ACTIVE;

  private boolean featured;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "primary_image")
  private String primaryImage;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProductImage> images = new ArrayList<>();

  @ManyToMany
  @JoinTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private java.util.Set<Tag> tags = new java.util.LinkedHashSet<>();

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  public Long getId() { return id; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Category getCategory() { return category; }
  public void setCategory(Category category) { this.category = category; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public BigDecimal getCompareAtPrice() { return compareAtPrice; }
  public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
  public String getBadge() { return badge; }
  public void setBadge(String badge) { this.badge = badge; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
  public String getMaterial() { return material; }
  public void setMaterial(String material) { this.material = material; }
  public String getDimensions() { return dimensions; }
  public void setDimensions(String dimensions) { this.dimensions = dimensions; }
  public int getStock() { return stock; }
  public void setStock(int stock) { this.stock = stock; }
  public int getReviewCount() { return reviewCount; }
  public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
  public BigDecimal getRating() { return rating; }
  public void setRating(BigDecimal rating) { this.rating = rating == null ? BigDecimal.valueOf(4.8) : rating; }
  public ProductStatus getStatus() { return status; }
  public void setStatus(ProductStatus status) { this.status = status; }
  public boolean isFeatured() { return featured; }
  public void setFeatured(boolean featured) { this.featured = featured; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
  public String getPrimaryImage() { return primaryImage; }
  public void setPrimaryImage(String primaryImage) { this.primaryImage = primaryImage; }
  public List<ProductImage> getImages() { return images; }
  public java.util.Set<Tag> getTags() { return tags; }
  public void setTags(java.util.Collection<Tag> values) { tags.clear(); tags.addAll(values); }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<ProductImage> getImagesInDisplayOrder() {
    return images.stream().sorted(Comparator.comparingInt(ProductImage::getDisplayOrder).thenComparing(ProductImage::getId)).toList();
  }
  public void replaceImages(List<ProductImageInput> rows) {
    images.clear();
    for (int i = 0; i < rows.size(); i++) {
      ProductImageInput row = rows.get(i);
      String url = row.url();
      if (url == null || url.isBlank()) continue;
      ProductImage image = new ProductImage();
      image.setProduct(this);
      image.setUrl(url.trim());
      image.setAltText(name);
      image.setDisplayOrder(i);
      image.setColor(row.color() == null || row.color().isBlank() ? null : row.color().trim());
      images.add(image);
    }
  }
}
