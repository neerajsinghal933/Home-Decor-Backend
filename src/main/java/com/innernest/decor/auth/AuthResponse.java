package com.innernest.decor.auth;

import com.innernest.decor.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
