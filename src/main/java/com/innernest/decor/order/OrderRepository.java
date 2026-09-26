package com.innernest.decor.order;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<Order> findWithItemsByOrderNumber(String orderNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"items", "items.product", "user"})
  @Query("select o from Order o where o.orderNumber = :orderNumber")
  Optional<Order> findWithItemsForUpdateByOrderNumber(@Param("orderNumber") String orderNumber);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

  @EntityGraph(attributePaths = {"items", "items.product"})
  List<Order> findAllByOrderByCreatedAtDesc();
}
