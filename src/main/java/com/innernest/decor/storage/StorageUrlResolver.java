package com.innernest.decor.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageUrlResolver {
  private final String publicBaseUrl;

  StorageUrlResolver(@Value("${app.storage.public-base-url}") String publicBaseUrl) {
    String normalized = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("app.storage.public-base-url must be a valid absolute URL", ex);
    }
    if (!uri.isAbsolute() || uri.getHost() == null) {
      throw new IllegalArgumentException("app.storage.public-base-url must be a valid absolute URL");
    }
    this.publicBaseUrl = normalized;
  }

  String urlFor(String key) {
    return publicBaseUrl + "/uploads/" + key;
  }
}
