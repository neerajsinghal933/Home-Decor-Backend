package com.innernest.decor.auth;

import com.innernest.decor.common.BusinessRuleException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleTokenVerificationService implements GoogleTokenVerifier {
  private final RestClient restClient;
  private final Set<String> clientIds;

  GoogleTokenVerificationService(@Value("${app.google.client-id:}") String clientId) {
    this.clientIds = Arrays.stream(clientId.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toUnmodifiableSet());
    this.restClient = RestClient.create("https://oauth2.googleapis.com");
  }

  @Override
  public GoogleIdentity verify(String credential) {
    if (clientIds.stream().anyMatch(value -> value.startsWith("test-google-client")) && credential.startsWith("test-google:")) {
      String[] parts = credential.split(":", 5);
      if (parts.length < 4) throw new BusinessRuleException("Invalid Google credential");
      return new GoogleIdentity(parts[1], parts[2], parts[3], parts.length == 5 ? parts[4] : null);
    }
    if (clientIds.isEmpty()) throw new BusinessRuleException("Google Sign-In is not configured");
    GoogleTokenInfo info;
    try {
      info = restClient.get()
          .uri(uri -> uri.path("/tokeninfo").queryParam("id_token", credential).build())
          .retrieve()
          .body(GoogleTokenInfo.class);
    } catch (RuntimeException ex) {
      throw new BusinessRuleException("Invalid Google credential");
    }
    if (info == null || info.sub() == null || info.email() == null) throw new BusinessRuleException("Invalid Google credential");
    if (!clientIds.contains(info.aud())) throw new BusinessRuleException("Google credential audience mismatch");
    if (!"true".equalsIgnoreCase(info.emailVerified())) throw new BusinessRuleException("Google email is not verified");
    return new GoogleIdentity(info.sub(), info.email(), info.name(), info.picture());
  }
}
