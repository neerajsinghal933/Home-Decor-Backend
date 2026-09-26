package com.innernest.decor.content;

import java.time.Instant;

public record NewsletterSubscriptionResponse(Long id, String email, Instant subscribedAt) {
  public static NewsletterSubscriptionResponse from(NewsletterSubscription subscription) {
    return new NewsletterSubscriptionResponse(subscription.getId(), subscription.getEmail(), subscription.getCreatedAt());
  }
}
