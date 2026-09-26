package com.innernest.decor.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
  Optional<RefundTransaction> findByServiceRequestId(Long requestId);
  Optional<RefundTransaction> findByRazorpayRefundId(String refundId);
  Optional<RefundTransaction> findByReceipt(String receipt);
  List<RefundTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
  List<RefundTransaction> findAllByOrderByCreatedAtDesc();
}
