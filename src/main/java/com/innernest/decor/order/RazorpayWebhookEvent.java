package com.innernest.decor.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "razorpay_webhook_events")
public class RazorpayWebhookEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, unique = true, length = 120)
  private String eventId;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @CreationTimestamp
  @Column(name = "processed_at", nullable = false, updatable = false)
  private Instant processedAt;

  protected RazorpayWebhookEvent() {
  }

  public RazorpayWebhookEvent(String eventId, String eventType) {
    this.eventId = eventId;
    this.eventType = eventType;
  }

  public String getEventId() { return eventId; }
  public String getEventType() { return eventType; }
  public Instant getProcessedAt() { return processedAt; }
}
