package com.innernest.decor.order;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private Order order;
  @Enumerated(EnumType.STRING) @Column(name = "previous_status") private OrderStatus previousStatus;
  @Enumerated(EnumType.STRING) @Column(name = "new_status", nullable = false) private OrderStatus newStatus;
  @Enumerated(EnumType.STRING) @Column(name = "actor_type", nullable = false) private OrderActorType actorType;
  @Column(name = "reference_type", length = 40) private String referenceType;
  @Column(name = "reference_id", length = 120) private String referenceId;
  @Column(length = 1000) private String note;
  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

  protected OrderStatusHistory() {}
  public OrderStatusHistory(Order order, OrderStatus previous, OrderStatus next, OrderActorType actor, String referenceType, String referenceId, String note) {
    this.order = order; this.previousStatus = previous; this.newStatus = next; this.actorType = actor;
    this.referenceType = referenceType; this.referenceId = referenceId; this.note = note;
  }
  public OrderStatus getPreviousStatus() { return previousStatus; }
  public OrderStatus getNewStatus() { return newStatus; }
  public OrderActorType getActorType() { return actorType; }
  public String getReferenceType() { return referenceType; }
  public String getReferenceId() { return referenceId; }
  public String getNote() { return note; }
  public Instant getCreatedAt() { return createdAt; }
}
