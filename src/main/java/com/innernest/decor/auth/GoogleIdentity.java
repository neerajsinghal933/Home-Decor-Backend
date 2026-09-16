package com.innernest.decor.auth;

public record GoogleIdentity(String subject, String email, String name, String profileImageUrl) {
}

