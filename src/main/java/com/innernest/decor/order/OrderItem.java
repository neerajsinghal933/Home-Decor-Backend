package com.innernest.decor.order;

import com.innernest.decor.catalog.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  private String productSku;
  private String productName;
  private String productImage;
  private String size;
  private String color;
  private BigDecimal unitPrice;
  private int qty;
  private BigDecimal lineTotal;

  public void setOrder(Order order) { this.order = order; }
  public void setProduct(Product product) { this.product = product; }
  public Product getProduct() { return product; }
  public String getProductSku() { return productSku; }
  public void setProductSku(String productSku) { this.productSku = productSku; }
  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }
  public String getProductImage() { return productImage; }
  public void setProductImage(String productImage) { this.productImage = productImage; }
  public String getSize() { return size; }
  public void setSize(String size) { this.size = size; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public int getQty() { return qty; }
  public void setQty(int qty) { this.qty = qty; }
  public BigDecimal getLineTotal() { return lineTotal; }
  public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
