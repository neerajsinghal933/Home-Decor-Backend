package com.innernest.decor.storage;

import java.io.InputStream;

public interface StorageService {
  StoredObject store(String key, String contentType, InputStream content);
}
