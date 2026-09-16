package com.innernest.decor.content;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String email;
  private String message;

  public void setName(String name) { this.name = name; }
  public void setEmail(String email) { this.email = email; }
  public void setMessage(String message) { this.message = message; }
}
