package com.innernest.decor.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {
  Optional<Tag> findByNameIgnoreCase(String name);
  List<Tag> findByIdIn(Collection<Long> ids);
  List<Tag> findByActiveTrueOrderByNameAsc();
  @Query("select t from Tag t where (:search is null or lower(t.name) like lower(concat('%', :search, '%')))")
  Page<Tag> search(String search, Pageable pageable);
  @Query("select count(p) from Product p join p.tags t where t.id = :tagId")
  long countProductsByTagId(Long tagId);
}
