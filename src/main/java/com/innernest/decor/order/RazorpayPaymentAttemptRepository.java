package com.innernest.decor.order;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RazorpayPaymentAttemptRepository extends JpaRepository<RazorpayPaymentAttempt, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "user", "completedOrder"})
  Optional<RazorpayPaymentAttempt> findFirstByUserIdAndStatusAndRazorpayOrderIdIsNotNullOrderByCreatedAtDesc(
      Long userId, RazorpayPaymentAttemptStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"items", "items.product", "user", "completedOrder"})
  Optional<RazorpayPaymentAttempt> findWithItemsByRazorpayOrderId(String razorpayOrderId);

  Optional<RazorpayPaymentAttempt> findByReceipt(String receipt);
}
