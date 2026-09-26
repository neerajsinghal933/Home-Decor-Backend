package com.innernest.decor.storage;

import java.io.InputStream;

public interface StorageService {
  StoredObject store(String key, String contentType, long contentLength, InputStream content);

  StoredContent load(String key);

  void delete(String key);
}
