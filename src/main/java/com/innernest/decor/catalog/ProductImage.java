package com.innernest.decor.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false, length = 500)
  private String url;

  @Column(name = "alt_text")
  private String altText;

  @Column(name = "display_order")
  private int displayOrder;

  @Column(length = 80)
  private String color;

  public Long getId() { return id; }
  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public String getAltText() { return altText; }
  public void setAltText(String altText) { this.altText = altText; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
}
