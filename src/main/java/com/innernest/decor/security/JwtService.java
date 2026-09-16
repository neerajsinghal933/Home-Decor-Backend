package com.innernest.decor.security;

import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final byte[] secret;
  private final long ttlSeconds;

  public JwtService(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.ttlSeconds = ttlMinutes * 60;
  }

  public String create(User user) {
    long now = Instant.now().getEpochSecond();
    String payload = user.getId() + "|" + user.getEmail() + "|" + user.getRole() + "|" + now + "|" + (now + ttlSeconds);
    String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return body + "." + sign(body);
  }

  public Optional<JwtPrincipal> parse(String token) {
    if (token == null || !token.contains(".")) return Optional.empty();
    String[] parts = token.split("\\.", 2);
    if (!constantTimeEquals(sign(parts[0]), parts[1])) return Optional.empty();
    try {
      String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      String[] values = payload.split("\\|", -1);
      long expiresAt = Long.parseLong(values[4]);
      if (expiresAt < Instant.now().getEpochSecond()) return Optional.empty();
      return Optional.of(new JwtPrincipal(Long.valueOf(values[0]), values[1], UserRole.valueOf(values[2])));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  private String sign(String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to sign JWT", ex);
    }
  }

  private boolean constantTimeEquals(String a, String b) {
    return MessageDigestCompat.equals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
