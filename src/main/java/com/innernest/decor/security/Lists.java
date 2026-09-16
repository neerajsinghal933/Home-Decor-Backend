package com.innernest.decor.security;

import java.util.List;

final class Lists {
  private Lists() {
  }

  static List<String> methods() {
    return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  }

  static List<String> headers() {
    return List.of("Authorization", "Content-Type", "X-Session-Id");
  }

  static List<String> exposed() {
    return List.of("Location");
  }
}
