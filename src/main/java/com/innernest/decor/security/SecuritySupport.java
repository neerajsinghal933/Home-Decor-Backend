package com.innernest.decor.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecuritySupport {
  private SecuritySupport() {
  }

  public static Optional<JwtPrincipal> currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) return Optional.empty();
    return Optional.of(principal);
  }
}
