package com.innernest.decor.catalog;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tags")
public class Tag {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true, length = 100)
  private String name;
  @Column(nullable = false)
  private boolean active = true;
  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;
  public Long getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
