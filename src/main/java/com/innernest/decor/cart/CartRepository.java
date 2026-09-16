package com.innernest.decor.cart;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {
  @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
  Optional<Cart> findWithItemsBySessionId(String sessionId);

  @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
  Optional<Cart> findWithItemsByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select cart from Cart cart where cart.sessionId = :sessionId")
  Optional<Cart> findBySessionIdForCheckout(@Param("sessionId") String sessionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select cart from Cart cart where cart.user.id = :userId")
  Optional<Cart> findByUserIdForCheckout(@Param("userId") Long userId);
}
