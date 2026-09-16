package com.innernest.decor.auth;

import com.innernest.decor.security.JwtPrincipal;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/google")
  AuthResponse google(@Valid @RequestBody GoogleAuthRequest request) {
    return authService.google(request);
  }

  @GetMapping("/me")
  UserResponse me() {
    JwtPrincipal principal = SecuritySupport.currentUser().orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    return authService.me(principal);
  }
}
