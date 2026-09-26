package com.innernest.decor.review;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
  Page<ProductReview> findByProductId(Long productId, Pageable pageable);
  Page<ProductReview> findByProductIdAndRating(Long productId, int rating, Pageable pageable);
  boolean existsByProductIdAndUserId(Long productId, Long userId);
  long countByProductId(Long productId);

  @Query("select avg(r.rating) from ProductReview r where r.product.id = :productId")
  Double averageRating(@Param("productId") Long productId);

  @Query("select r.rating, count(r) from ProductReview r where r.product.id = :productId group by r.rating")
  List<Object[]> ratingCounts(@Param("productId") Long productId);
}
