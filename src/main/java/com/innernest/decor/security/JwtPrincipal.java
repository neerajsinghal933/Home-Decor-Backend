package com.innernest.decor.security;

import com.innernest.decor.user.UserRole;

public record JwtPrincipal(Long id, String email, UserRole role) {
}
