package com.innernest.decor.order;

import com.innernest.decor.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "order_service_requests")
public class OrderServiceRequest {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private Order order;
  @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false) private OrderRequestType type;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private OrderRequestStatus status = OrderRequestStatus.REQUESTED;
  @Enumerated(EnumType.STRING) @Column(name = "previous_order_status", nullable = false) private OrderStatus previousOrderStatus;
  @Column(nullable = false, length = 300) private String reason;
  @Column(length = 1000) private String details;
  @Column(name = "admin_note", length = 1000) private String adminNote;
  @CreationTimestamp @Column(name = "requested_at", nullable = false, updatable = false) private Instant requestedAt;
  @Column(name = "reviewed_at") private Instant reviewedAt;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_user_id") private User reviewedBy;

  public Long getId() { return id; }
  public Order getOrder() { return order; }
  public void setOrder(Order order) { this.order = order; }
  public OrderRequestType getType() { return type; }
  public void setType(OrderRequestType type) { this.type = type; }
  public OrderRequestStatus getStatus() { return status; }
  public void setStatus(OrderRequestStatus status) { this.status = status; }
  public OrderStatus getPreviousOrderStatus() { return previousOrderStatus; }
  public void setPreviousOrderStatus(OrderStatus value) { this.previousOrderStatus = value; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public String getAdminNote() { return adminNote; }
  public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
  public Instant getRequestedAt() { return requestedAt; }
  public Instant getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
  public User getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
}
