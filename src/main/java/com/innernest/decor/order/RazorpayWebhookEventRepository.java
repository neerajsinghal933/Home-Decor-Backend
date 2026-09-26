package com.innernest.decor.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayWebhookEventRepository extends JpaRepository<RazorpayWebhookEvent, Long> {
  boolean existsByEventId(String eventId);
}
