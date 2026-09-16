package com.innernest.decor.promo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "promo_codes")
public class PromoCode {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 80)
  private String code;

  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false)
  private DiscountType discountType = DiscountType.PERCENT;

  @Column(name = "discount_value", nullable = false)
  private BigDecimal discountValue;

  @Column(name = "minimum_order_amount", nullable = false)
  private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

  @Column(nullable = false)
  private boolean active = true;

  public Long getId() { return id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public DiscountType getDiscountType() { return discountType; }
  public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
  public BigDecimal getDiscountValue() { return discountValue; }
  public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
  public BigDecimal getMinimumOrderAmount() { return minimumOrderAmount; }
  public void setMinimumOrderAmount(BigDecimal minimumOrderAmount) { this.minimumOrderAmount = minimumOrderAmount; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
