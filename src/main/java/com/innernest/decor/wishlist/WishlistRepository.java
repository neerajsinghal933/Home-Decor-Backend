package com.innernest.decor.wishlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
  @EntityGraph(attributePaths = {"product", "product.category"})
  List<WishlistItem> findByUserIdOrderByIdDesc(Long userId);
  Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);
  boolean existsByUserIdAndProductId(Long userId, Long productId);
}

