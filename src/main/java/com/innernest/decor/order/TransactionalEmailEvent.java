package com.innernest.decor.order;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactional_email_events")
public class TransactionalEmailEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "event_key", nullable = false, unique = true, length = 180) private String eventKey;
  @Column(name = "event_type", nullable = false, length = 50) private String eventType;
  @Column(nullable = false, length = 180) private String recipient;
  @Column(nullable = false) private String subject;
  @Column(nullable = false, columnDefinition = "TEXT") private String body;
  @Enumerated(EnumType.STRING) @Column(name = "delivery_status", nullable = false) private TransactionalEmailStatus deliveryStatus = TransactionalEmailStatus.QUEUED;
  @Column(nullable = false) private int attempts;
  @Column(name = "last_error", length = 500) private String lastError;
  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
  @Column(name = "sent_at") private Instant sentAt;

  protected TransactionalEmailEvent() {}
  TransactionalEmailEvent(String key, String type, String recipient, String subject, String body) {
    this.eventKey = key; this.eventType = type; this.recipient = recipient; this.subject = subject; this.body = body;
  }
  public Long getId() { return id; }
  public String getEventKey() { return eventKey; }
  public String getRecipient() { return recipient; }
  public String getSubject() { return subject; }
  public String getBody() { return body; }
  public TransactionalEmailStatus getDeliveryStatus() { return deliveryStatus; }
  public void setDeliveryStatus(TransactionalEmailStatus value) { this.deliveryStatus = value; }
  public int getAttempts() { return attempts; }
  public void setAttempts(int value) { this.attempts = value; }
  public String getLastError() { return lastError; }
  public void setLastError(String value) { this.lastError = value; }
  public Instant getSentAt() { return sentAt; }
  public void setSentAt(Instant value) { this.sentAt = value; }
}
