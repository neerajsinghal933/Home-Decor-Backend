package com.innernest.decor.user;

public record UserResponse(Long id, String name, String email, String phone, String profileImageUrl, UserRole role) {
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getProfileImageUrl(), user.getRole());
  }
}
