package com.innernest.decor.cart;

import com.innernest.decor.catalog.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items")
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cart_id")
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  private int qty;
  private String size;
  private String color;

  public Long getId() { return id; }
  public Cart getCart() { return cart; }
  public void setCart(Cart cart) { this.cart = cart; }
  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }
  public int getQty() { return qty; }
  public void setQty(int qty) { this.qty = qty; }
  public String getSize() { return size; }
  public void setSize(String size) { this.size = size; }
  public String getColor() { return color; }
  public void setColor(String color) { this.color = color; }
}
