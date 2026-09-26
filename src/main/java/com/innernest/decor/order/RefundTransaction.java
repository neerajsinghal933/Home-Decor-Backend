package com.innernest.decor.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "refund_transactions")
public class RefundTransaction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private Order order;
  @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_request_id", unique = true) private OrderServiceRequest serviceRequest;
  @Column(name = "razorpay_payment_id", nullable = false, length = 120) private String razorpayPaymentId;
  @Column(name = "razorpay_refund_id", unique = true, length = 120) private String razorpayRefundId;
  @Column(name = "idempotency_key", nullable = false, unique = true, length = 80) private String idempotencyKey;
  @Column(nullable = false, unique = true, length = 80) private String receipt;
  @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
  @Column(nullable = false, length = 10) private String currency = "INR";
  @Enumerated(EnumType.STRING) @Column(nullable = false) private RefundStatus status = RefundStatus.CREATING;
  @Column(name = "failure_reason", length = 500) private String failureReason;
  @Column(name = "refund_reference", length = 160) private String refundReference;
  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
  @Column(name = "initiated_at") private Instant initiatedAt;
  @Column(name = "completed_at") private Instant completedAt;
  @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

  public Long getId() { return id; }
  public Order getOrder() { return order; }
  public void setOrder(Order order) { this.order = order; }
  public OrderServiceRequest getServiceRequest() { return serviceRequest; }
  public void setServiceRequest(OrderServiceRequest value) { this.serviceRequest = value; }
  public String getRazorpayPaymentId() { return razorpayPaymentId; }
  public void setRazorpayPaymentId(String value) { this.razorpayPaymentId = value; }
  public String getRazorpayRefundId() { return razorpayRefundId; }
  public void setRazorpayRefundId(String value) { this.razorpayRefundId = value; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
  public String getReceipt() { return receipt; }
  public void setReceipt(String value) { this.receipt = value; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal value) { this.amount = value; }
  public String getCurrency() { return currency; }
  public RefundStatus getStatus() { return status; }
  public void setStatus(RefundStatus value) { this.status = value; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String value) { this.failureReason = value; }
  public String getRefundReference() { return refundReference; }
  public void setRefundReference(String value) { this.refundReference = value; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getInitiatedAt() { return initiatedAt; }
  public void setInitiatedAt(Instant value) { this.initiatedAt = value; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant value) { this.completedAt = value; }
}
