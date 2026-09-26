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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_number", nullable = false, unique = true)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "guest_session_id")
  private String guestSessionId;

  private String customerName;
  private String customerPhone;
  private String customerEmail;
  private String address;
  private String city;
  private String state;
  private String pincode;
  private String landmark;
  private String paymentMethod;

  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  private String deliveryMethod;
  private String estimatedDelivery;

  @Enumerated(EnumType.STRING)
  private OrderStatus status = OrderStatus.PLACED;

  private BigDecimal subtotal;
  private BigDecimal shipping;
  private BigDecimal estimatedTax;
  private BigDecimal discountAmount = BigDecimal.ZERO;
  private BigDecimal total;
  private String promoCode;
  private String paymentProvider;
  private String razorpayOrderId;
  private String razorpayPaymentId;
  private String razorpaySignature;

  @Column(name = "paid_at")
  private Instant paidAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  public Long getId() { return id; }
  public String getOrderNumber() { return orderNumber; }
  public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public String getGuestSessionId() { return guestSessionId; }
  public void setGuestSessionId(String guestSessionId) { this.guestSessionId = guestSessionId; }
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
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
  public String getDeliveryMethod() { return deliveryMethod; }
  public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
  public String getEstimatedDelivery() { return estimatedDelivery; }
  public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
  public OrderStatus getStatus() { return status; }
  public void setStatus(OrderStatus status) { this.status = status; }
  public BigDecimal getSubtotal() { return subtotal; }
  public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
  public BigDecimal getShipping() { return shipping; }
  public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
  public BigDecimal getEstimatedTax() { return estimatedTax; }
  public void setEstimatedTax(BigDecimal estimatedTax) { this.estimatedTax = estimatedTax; }
  public BigDecimal getDiscountAmount() { return discountAmount; }
  public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
  public String getPromoCode() { return promoCode; }
  public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
  public String getPaymentProvider() { return paymentProvider; }
  public void setPaymentProvider(String paymentProvider) { this.paymentProvider = paymentProvider; }
  public String getRazorpayOrderId() { return razorpayOrderId; }
  public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
  public String getRazorpayPaymentId() { return razorpayPaymentId; }
  public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
  public String getRazorpaySignature() { return razorpaySignature; }
  public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
  public Instant getPaidAt() { return paidAt; }
  public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<OrderItem> getItems() { return items; }
}
