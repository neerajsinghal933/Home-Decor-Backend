package com.innernest.decor.storage;

import com.innernest.decor.common.BusinessRuleException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public final class ImageUploadValidator {
  private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
  private static final Map<String, Set<String>> MIME_EXTENSIONS = Map.of(
      "image/jpeg", Set.of("jpg", "jpeg"),
      "image/png", Set.of("png"),
      "image/webp", Set.of("webp"),
      "image/gif", Set.of("gif"));

  private ImageUploadValidator() {
  }

  public static ValidatedImage validate(MultipartFile file, String emptyMessage) throws IOException {
    if (file == null || file.isEmpty()) throw new BusinessRuleException(emptyMessage);

    String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String extension = extension(original);
    if (!EXTENSIONS.contains(extension)) {
      throw new BusinessRuleException("Only JPG, PNG, WEBP or GIF images are allowed");
    }

    String contentType = file.getContentType() == null
        ? ""
        : file.getContentType().toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    if (!MIME_EXTENSIONS.getOrDefault(contentType, Set.of()).contains(extension)) {
      throw new BusinessRuleException("Uploaded file must be an image");
    }

    byte[] header;
    try (InputStream input = file.getInputStream()) {
      header = input.readNBytes(12);
    }
    if (!hasExpectedSignature(contentType, header)) {
      throw new BusinessRuleException("Uploaded file must be an image");
    }
    return new ValidatedImage(extension, contentType);
  }

  private static boolean hasExpectedSignature(String contentType, byte[] bytes) {
    return switch (contentType) {
      case "image/jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
      case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
      case "image/gif" -> asciiAt(bytes, 0, "GIF87a") || asciiAt(bytes, 0, "GIF89a");
      case "image/webp" -> asciiAt(bytes, 0, "RIFF") && asciiAt(bytes, 8, "WEBP");
      default -> false;
    };
  }

  private static boolean startsWith(byte[] bytes, int... expected) {
    if (bytes.length < expected.length) return false;
    for (int i = 0; i < expected.length; i++) {
      if ((bytes[i] & 0xff) != expected[i]) return false;
    }
    return true;
  }

  private static boolean asciiAt(byte[] bytes, int offset, String expected) {
    if (bytes.length < offset + expected.length()) return false;
    for (int i = 0; i < expected.length(); i++) {
      if (bytes[offset + i] != (byte) expected.charAt(i)) return false;
    }
    return true;
  }

  private static String extension(String filename) {
    int index = filename.lastIndexOf('.');
    return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
  }

  public record ValidatedImage(String extension, String contentType) {
  }
}
