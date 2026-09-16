package com.innernest.decor.security;

import java.security.MessageDigest;

final class MessageDigestCompat {
  private MessageDigestCompat() {
  }

  static boolean equals(byte[] left, byte[] right) {
    return MessageDigest.isEqual(left, right);
  }
}
