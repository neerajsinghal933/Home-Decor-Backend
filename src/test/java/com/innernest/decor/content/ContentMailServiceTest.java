package com.innernest.decor.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class ContentMailServiceTest {
  @Test
  void contactSendsTeamNotificationAndSenderConfirmation() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ContentMailService service = new ContentMailService(sender, true, "innernestofficial@gmail.com", "innernestofficial@gmail.com");
    ArgumentCaptor<SimpleMailMessage[]> messages = ArgumentCaptor.forClass(SimpleMailMessage[].class);

    service.sendContactEmails("Asha", "asha@example.com", "I need help");

    verify(sender).send(messages.capture());
    assertEquals(2, messages.getValue().length);
    assertEquals("innernestofficial@gmail.com", messages.getValue()[0].getTo()[0]);
    assertEquals("asha@example.com", messages.getValue()[0].getReplyTo());
    assertEquals("asha@example.com", messages.getValue()[1].getTo()[0]);
    assertTrue(messages.getValue()[1].getText().contains("I need help"));
  }

  @Test
  void disabledMailDoesNotContactSmtpServer() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ContentMailService service = new ContentMailService(sender, false, "innernestofficial@gmail.com", "innernestofficial@gmail.com");

    service.sendContactEmails("Asha", "asha@example.com", "I need help");

    verify(sender, never()).send(any(SimpleMailMessage[].class));
  }
}
