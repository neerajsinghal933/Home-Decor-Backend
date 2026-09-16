package com.innernest.decor.auth;

public interface GoogleTokenVerifier {
  GoogleIdentity verify(String credential);
}

