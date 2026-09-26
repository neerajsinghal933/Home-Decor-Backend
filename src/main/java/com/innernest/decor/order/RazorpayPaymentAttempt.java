package com.innernest.decor.order;

import com.innernest.decor.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "razorpay_payment_attempts")
public class RazorpayPaymentAttempt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String receipt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "razorpay_order_id", unique = true, length = 120)
  private String razorpayOrderId;

  @Column(name = "razorpay_payment_id", unique = true, length = 120)
  private String razorpayPaymentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RazorpayPaymentAttemptStatus status = RazorpayPaymentAttemptStatus.PENDING;

  private String customerName;
  private String customerPhone;
  private String customerEmail;
  private String address;
  private String city;
  private String state;
  private String pincode;
  private String landmark;
  private String deliveryMethod;
  private String estimatedDelivery;
  private BigDecimal subtotal;
  private BigDecimal shipping;
  private BigDecimal estimatedTax;
  private BigDecimal discountAmount = BigDecimal.ZERO;
  private BigDecimal total;
  private String promoCode;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "completed_order_id", unique = true)
  private Order completedOrder;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RazorpayPaymentAttemptItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public String getReceipt() { return receipt; }
  public void setReceipt(String receipt) { this.receipt = receipt; }
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public String getRazorpayOrderId() { return razorpayOrderId; }
  public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
  public String getRazorpayPaymentId() { return razorpayPaymentId; }
  public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
  public RazorpayPaymentAttemptStatus getStatus() { return status; }
  public void setStatus(RazorpayPaymentAttemptStatus status) { this.status = status; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String customerName) { this.customerName = customerName; }
  public String getCustomerPhone() { return customerPhone; }
  public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
  public String getCustomerEmail() { return customerEmail; }
  public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getState() { return state; }
  public void setState(String state) { this.state = state; }
  public String getPincode() { return pincode; }
  public void setPincode(String pincode) { this.pincode = pincode; }
  public String getLandmark() { return landmark; }
  public void setLandmark(String landmark) { this.landmark = landmark; }
  public String getDeliveryMethod() { return deliveryMethod; }
  public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
  public String getEstimatedDelivery() { return estimatedDelivery; }
  public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
  public BigDecimal getSubtotal() { return subtotal; }
  public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
  public BigDecimal getShipping() { return shipping; }
  public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
  public BigDecimal getEstimatedTax() { return estimatedTax; }
  public void setEstimatedTax(BigDecimal estimatedTax) { this.estimatedTax = estimatedTax; }
  public BigDecimal getDiscountAmount() { return discountAmount; }
  public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public String getPromoCode() { return promoCode; }
  public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
  public Order getCompletedOrder() { return completedOrder; }
  public void setCompletedOrder(Order completedOrder) { this.completedOrder = completedOrder; }
  public Instant getCreatedAt() { return createdAt; }
  public List<RazorpayPaymentAttemptItem> getItems() { return items; }
}
