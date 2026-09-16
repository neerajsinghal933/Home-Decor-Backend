package com.innernest.decor.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev", "test"})
public class LocalStorageService implements StorageService {
  private final Path root;

  public LocalStorageService(@Value("${app.storage.local-path:uploads}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  @Override
  public StoredObject store(String key, String contentType, InputStream content) {
    try {
      Path target = root.resolve(key).normalize();
      if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
      Files.createDirectories(target.getParent());
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
      return new StoredObject(key, "/uploads/" + key, contentType);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to store object", ex);
    }
  }
}
