package com.innernest.decor.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.innernest.decor.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev", "test"})
public class LocalStorageService implements StorageService {
  private final Path root;
  private final StorageUrlResolver urls;

  public LocalStorageService(@Value("${app.storage.local-path:uploads}") String root, StorageUrlResolver urls) {
    this.root = Path.of(root).toAbsolutePath().normalize();
    this.urls = urls;
  }

  @Override
  public StoredObject store(String key, String contentType, long contentLength, InputStream content) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
      return new StoredObject(key, urls.urlFor(key), contentType);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to store object", ex);
    }
  }

  @Override
  public StoredContent load(String key) {
    Path target = resolve(key);
    if (!Files.isRegularFile(target)) throw new ResourceNotFoundException("Image not found");
    try {
      String contentType = Files.probeContentType(target);
      return new StoredContent(
          Files.newInputStream(target),
          contentType == null ? "application/octet-stream" : contentType,
          Files.size(target));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to load object", ex);
    }
  }

  @Override
  public void delete(String key) {
    Path target = resolve(key);
    try {
      Files.deleteIfExists(target);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to delete object", ex);
    }
  }

  private Path resolve(String key) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("Storage key is required");
    Path target = root.resolve(key).normalize();
    if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
    return target;
  }
}
