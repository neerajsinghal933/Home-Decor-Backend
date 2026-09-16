package com.innernest.decor.content;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContentController {
  private final NewsletterRepository newsletters;
  private final ContactMessageRepository contacts;

  ContentController(NewsletterRepository newsletters, ContactMessageRepository contacts) {
    this.newsletters = newsletters;
    this.contacts = contacts;
  }

  @GetMapping("/health")
  MessageResponse health() {
    return new MessageResponse("ok");
  }

  @PostMapping("/newsletter")
  @ResponseStatus(HttpStatus.CREATED)
  MessageResponse newsletter(@Valid @RequestBody NewsletterRequest request) {
    if (!newsletters.existsByEmailIgnoreCase(request.email())) {
      NewsletterSubscription subscription = new NewsletterSubscription();
      subscription.setEmail(request.email().trim().toLowerCase());
      newsletters.save(subscription);
    }
    return new MessageResponse("Subscribed");
  }

  @PostMapping("/contact")
  @ResponseStatus(HttpStatus.CREATED)
  MessageResponse contact(@Valid @RequestBody ContactRequest request) {
    ContactMessage message = new ContactMessage();
    message.setName(request.name().trim());
    message.setEmail(request.email().trim().toLowerCase());
    message.setMessage(request.message().trim());
    contacts.save(message);
    return new MessageResponse("Message received");
  }
}
