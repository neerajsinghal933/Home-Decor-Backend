package com.innernest.decor.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionalEmailEventRepository extends JpaRepository<TransactionalEmailEvent, Long> {
  boolean existsByEventKey(String eventKey);
}
