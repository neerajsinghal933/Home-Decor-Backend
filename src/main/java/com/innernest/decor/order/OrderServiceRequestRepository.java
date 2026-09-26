package com.innernest.decor.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderServiceRequestRepository extends JpaRepository<OrderServiceRequest, Long> {
  Optional<OrderServiceRequest> findFirstByOrderIdAndStatusOrderByRequestedAtDesc(Long orderId, OrderRequestStatus status);
  Optional<OrderServiceRequest> findFirstByOrderIdOrderByRequestedAtDesc(Long orderId);
  @EntityGraph(attributePaths = {"order", "order.user"})
  List<OrderServiceRequest> findAllByOrderByRequestedAtDesc();
  @EntityGraph(attributePaths = {"order", "order.user"})
  List<OrderServiceRequest> findByTypeAndStatusOrderByRequestedAtDesc(OrderRequestType type, OrderRequestStatus status);
}
