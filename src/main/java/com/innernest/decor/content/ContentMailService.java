package com.innernest.decor.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContentMailService {
  private static final Logger log = LoggerFactory.getLogger(ContentMailService.class);

  private final JavaMailSender sender;
  private final boolean enabled;
  private final String from;
  private final String contactTo;

  ContentMailService(JavaMailSender sender,
                     @Value("${app.mail.enabled:false}") boolean enabled,
                     @Value("${app.mail.from:innernestofficial@gmail.com}") String from,
                     @Value("${app.mail.contact-to:innernestofficial@gmail.com}") String contactTo) {
    this.sender = sender;
    this.enabled = enabled;
    this.from = from;
    this.contactTo = contactTo;
  }

  public void sendContactEmails(String name, String email, String body) {
    if (!enabled) {
      log.info("Contact email delivery is disabled; message from {} was stored only", email);
      return;
    }

    SimpleMailMessage team = message(contactTo, "New contact message from " + name,
        "Name: " + name + "\nEmail: " + email + "\n\nMessage:\n" + body);
    team.setReplyTo(email);
    SimpleMailMessage confirmation = message(email, "We received your message — Inner Nest Decor",
        "Hi " + name + ",\n\nThank you for contacting Inner Nest Decor. We have received your message and our team will reply as soon as possible.\n\nYour message:\n" + body + "\n\nWarmly,\nInner Nest Decor");
    send(team, confirmation);
  }

  public void sendSubscriptionConfirmation(String name, String email) {
    if (!enabled) return;
    send(message(email, "Welcome to Inner Nest Decor",
        "Hi " + name + ",\n\nYou're subscribed. We'll send you new arrivals, curated collections and special updates.\n\nWarmly,\nInner Nest Decor"));
  }

  private SimpleMailMessage message(String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    return message;
  }

  private void send(SimpleMailMessage... messages) {
    try {
      sender.send(messages);
    } catch (MailException exception) {
      log.error("Email delivery failed", exception);
      throw exception;
    }
  }
}
