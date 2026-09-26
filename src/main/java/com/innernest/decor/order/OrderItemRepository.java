package com.innernest.decor.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  @Query("""
      select case when count(item) > 0 then true else false end
      from OrderItem item
      where item.product.id = :productId
        and item.order.user.id = :userId
        and item.order.paymentStatus not in (com.innernest.decor.order.PaymentStatus.PENDING, com.innernest.decor.order.PaymentStatus.FAILED)
        and item.order.status <> com.innernest.decor.order.OrderStatus.CANCELLED
      """)
  boolean hasPurchased(@Param("productId") Long productId, @Param("userId") Long userId);
}
