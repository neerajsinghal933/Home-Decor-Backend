package com.innernest.decor.order;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<Order> findWithItemsByOrderNumber(String orderNumber);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findAllByOrderByCreatedAtDesc();
}
