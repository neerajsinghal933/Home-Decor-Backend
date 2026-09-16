package com.innernest.decor.auth;

import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.JwtPrincipal;
import com.innernest.decor.security.JwtService;
import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRepository;
import com.innernest.decor.user.UserResponse;
import com.innernest.decor.user.UserRole;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final JwtService jwtService;
  private final GoogleTokenVerifier googleTokenVerifier;
  private final Set<String> adminEmails;

  AuthService(UserRepository users, JwtService jwtService, GoogleTokenVerifier googleTokenVerifier,
              @Value("${app.admin.emails:}") String adminEmails) {
    this.users = users;
    this.jwtService = jwtService;
    this.googleTokenVerifier = googleTokenVerifier;
    this.adminEmails = Arrays.stream(adminEmails.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Transactional
  public AuthResponse google(GoogleAuthRequest request) {
    GoogleIdentity identity = googleTokenVerifier.verify(request.credential());
    User user = users.findByGoogleSubjectId(identity.subject())
        .or(() -> users.findByEmailIgnoreCase(identity.email()))
        .orElseGet(User::new);
    if (user.getId() == null) {
      user.setEmail(identity.email().trim().toLowerCase());
      user.setRole(UserRole.USER);
    } else if (!user.isEnabled()) {
      throw new BusinessRuleException("User account is disabled");
    }
    if (adminEmails.contains("*") || adminEmails.contains(identity.email().trim().toLowerCase())) {
      user.setRole(UserRole.ADMIN);
    }
    user.setGoogleSubjectId(identity.subject());
    user.setName(identity.name() == null || identity.name().isBlank() ? identity.email() : identity.name().trim());
    user.setProfileImageUrl(identity.profileImageUrl());
    user.setLastLoginAt(Instant.now());
    users.save(user);
    return new AuthResponse(jwtService.create(user), UserResponse.from(user));
  }

  @Transactional(readOnly = true)
  public UserResponse me(JwtPrincipal principal) {
    return users.findById(principal.id()).map(UserResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }
}
