package com.innernest.decor.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("select p from Product p where p.id = :id")
  Optional<Product> findForInventoryUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = {"category", "images"})
  Optional<Product> findWithImagesById(Long id);

  @EntityGraph(attributePaths = {"category", "images"})
  Optional<Product> findWithImagesBySlugAndStatus(String slug, ProductStatus status);

  @EntityGraph(attributePaths = {"category", "images"})
  List<Product> findAllByOrderByIdAsc();

  @EntityGraph(attributePaths = {"category", "images"})
  List<Product> findAllByOrderByDisplayOrderAscIdAsc();

  @Query("""
      select distinct p from Product p
      join fetch p.category c
      left join fetch p.images i
      where p.status = com.innernest.decor.catalog.ProductStatus.ACTIVE
        and (:category is null or c.name = :category)
        and (:search is null or lower(p.name) like lower(concat('%', :search, '%'))
          or lower(c.name) like lower(concat('%', :search, '%'))
          or lower(coalesce(p.color, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(p.badge, '')) like lower(concat('%', :search, '%')))
      """)
  List<Product> search(@Param("search") String search, @Param("category") String category);
}
