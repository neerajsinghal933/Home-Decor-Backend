package com.innernest.decor.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record GoogleTokenInfo(
    String sub,
    String email,
    String name,
    String picture,
    String aud,
    @JsonProperty("email_verified") String emailVerified) {
}

