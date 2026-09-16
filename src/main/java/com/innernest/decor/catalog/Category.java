package com.innernest.decor.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private String name;

  private String description;
  private String image;
  private boolean active = true;

  @Column(name = "display_order")
  private int displayOrder;

  public Long getId() { return id; }
  public String getSlug() { return slug; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getImage() { return image; }
  public boolean isActive() { return active; }
  public int getDisplayOrder() { return displayOrder; }
}
