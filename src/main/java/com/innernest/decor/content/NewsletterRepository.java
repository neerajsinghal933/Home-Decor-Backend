package com.innernest.decor.content;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterRepository extends JpaRepository<NewsletterSubscription, Long> {
  boolean existsByEmailIgnoreCase(String email);
}
