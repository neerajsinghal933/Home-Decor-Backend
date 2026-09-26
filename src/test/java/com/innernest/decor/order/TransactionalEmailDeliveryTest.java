package com.innernest.decor.order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class TransactionalEmailDeliveryTest {
  @Test
  void smtpFailureIsRecordedAndDoesNotEscapeIntoFinancialWorkflow() {
    TransactionalEmailEventRepository repository = mock(TransactionalEmailEventRepository.class);
    JavaMailSender sender = mock(JavaMailSender.class);
    TransactionalEmailEvent event = new TransactionalEmailEvent("REFUND_PROCESSED:1", "REFUND_PROCESSED",
        "customer@example.com", "Refund processed", "Safe customer message");
    when(repository.findById(1L)).thenReturn(Optional.of(event));
    doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(org.springframework.mail.SimpleMailMessage.class));

    TransactionalEmailDelivery delivery = new TransactionalEmailDelivery(repository, sender, true, "team@example.com");
    assertDoesNotThrow(() -> delivery.deliver(1L));
    assertEquals(TransactionalEmailStatus.FAILED, event.getDeliveryStatus());
    assertEquals("Email delivery failed", event.getLastError());
  }
}
