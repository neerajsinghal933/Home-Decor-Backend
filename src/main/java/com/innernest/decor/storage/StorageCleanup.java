package com.innernest.decor.storage;

import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class StorageCleanup {
  private static final Logger log = LoggerFactory.getLogger(StorageCleanup.class);
  private static final String UPLOAD_PATH = "/uploads/";
  private final StorageService storage;

  StorageCleanup(StorageService storage) {
    this.storage = storage;
  }

  public void deleteAfterCommit(String url, String requiredPrefix) {
    keyFromUrl(url, requiredPrefix).ifPresent(key -> {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            deleteQuietly(key);
          }
        });
      } else {
        deleteQuietly(key);
      }
    });
  }

  public void replaceAfterTransaction(String oldUrl, String newKey, String requiredPrefix) {
    Optional<String> oldKey = keyFromUrl(oldUrl, requiredPrefix).filter(key -> !key.equals(newKey));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          oldKey.ifPresent(StorageCleanup.this::deleteQuietly);
        }

        @Override
        public void afterCompletion(int status) {
          if (status != TransactionSynchronization.STATUS_COMMITTED) deleteQuietly(newKey);
        }
      });
    } else {
      oldKey.ifPresent(this::deleteQuietly);
    }
  }

  public static Optional<String> keyFromUrl(String url, String requiredPrefix) {
    if (url == null || url.isBlank()) return Optional.empty();
    try {
      String path = URI.create(url.trim()).getPath();
      if (path == null || !path.startsWith(UPLOAD_PATH)) return Optional.empty();
      String key = path.substring(UPLOAD_PATH.length());
      if (!key.startsWith(requiredPrefix) || key.contains("..")) return Optional.empty();
      return Optional.of(key);
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private void deleteQuietly(String key) {
    try {
      storage.delete(key);
    } catch (RuntimeException ex) {
      log.warn("Unable to delete obsolete stored object {}", key, ex);
    }
  }
}
