package com.innernest.decor.content;

import jakarta.validation.Valid;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRepository;
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
  private final UserRepository users;
  private final ContentMailService mail;

  ContentController(NewsletterRepository newsletters, ContactMessageRepository contacts, UserRepository users, ContentMailService mail) {
    this.newsletters = newsletters;
    this.contacts = contacts;
    this.users = users;
    this.mail = mail;
  }

  @GetMapping("/health")
  MessageResponse health() {
    return new MessageResponse("ok");
  }

  @PostMapping("/newsletter")
  @ResponseStatus(HttpStatus.CREATED)
  MessageResponse newsletter() {
    User user = currentUser();
    if (!newsletters.existsByEmailIgnoreCase(user.getEmail())) {
      NewsletterSubscription subscription = new NewsletterSubscription();
      subscription.setEmail(user.getEmail().trim().toLowerCase());
      newsletters.save(subscription);
      mail.sendSubscriptionConfirmation(user.getName(), user.getEmail());
    }
    return new MessageResponse("Subscribed");
  }

  @PostMapping("/contact")
  @ResponseStatus(HttpStatus.CREATED)
  MessageResponse contact(@Valid @RequestBody ContactRequest request) {
    User user = currentUser();
    ContactMessage message = new ContactMessage();
    message.setName(user.getName());
    message.setEmail(user.getEmail().trim().toLowerCase());
    message.setMessage(request.message().trim());
    contacts.save(message);
    mail.sendContactEmails(user.getName(), user.getEmail(), request.message().trim());
    return new MessageResponse("Message received");
  }

  private User currentUser() {
    Long userId = SecuritySupport.currentUser().orElseThrow(() -> new ResourceNotFoundException("User not found")).id();
    return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }
}
