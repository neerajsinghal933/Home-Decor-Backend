package com.innernest.decor.order;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalEmailDelivery {
  private static final Logger log = LoggerFactory.getLogger(TransactionalEmailDelivery.class);
  private final TransactionalEmailEventRepository events;
  private final JavaMailSender sender;
  private final boolean enabled;
  private final String from;

  TransactionalEmailDelivery(TransactionalEmailEventRepository events, JavaMailSender sender,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:innernestofficial@gmail.com}") String from) {
    this.events = events; this.sender = sender; this.enabled = enabled; this.from = from;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deliver(Long id) {
    TransactionalEmailEvent event = events.findById(id).orElse(null);
    if (event == null || event.getDeliveryStatus() != TransactionalEmailStatus.QUEUED) return;
    event.setAttempts(event.getAttempts() + 1);
    if (!enabled) {
      event.setDeliveryStatus(TransactionalEmailStatus.DISABLED);
      return;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from); message.setTo(event.getRecipient()); message.setSubject(event.getSubject()); message.setText(event.getBody());
      sender.send(message);
      event.setDeliveryStatus(TransactionalEmailStatus.SENT);
      event.setSentAt(Instant.now());
      event.setLastError(null);
    } catch (Exception ex) {
      event.setDeliveryStatus(TransactionalEmailStatus.FAILED);
      event.setLastError("Email delivery failed");
      log.error("Transactional email delivery failed for event {}", event.getEventKey());
    }
  }
}
