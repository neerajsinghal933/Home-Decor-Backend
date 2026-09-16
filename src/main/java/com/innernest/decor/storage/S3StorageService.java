package com.innernest.decor.storage;

import java.io.InputStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class S3StorageService implements StorageService {
  @Override
  public StoredObject store(String key, String contentType, InputStream content) {
    throw new UnsupportedOperationException("Configure cloud object storage before enabling product image uploads in production.");
  }
}
