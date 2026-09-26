package com.innernest.decor.storage;

import java.io.IOException;
import java.io.InputStream;

public record StoredContent(InputStream inputStream, String contentType, long contentLength) implements AutoCloseable {
  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
