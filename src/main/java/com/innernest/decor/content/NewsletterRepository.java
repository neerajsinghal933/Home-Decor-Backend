package com.innernest.decor.content;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsletterRepository extends JpaRepository<NewsletterSubscription, Long> {
  boolean existsByEmailIgnoreCase(String email);
  List<NewsletterSubscription> findAllByOrderByCreatedAtDesc();
}
