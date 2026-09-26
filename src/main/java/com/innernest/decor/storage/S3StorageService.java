package com.innernest.decor.storage;

import com.innernest.decor.common.ResourceNotFoundException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Profile("prod")
public class S3StorageService implements StorageService {
  private final S3Client s3;
  private final String bucket;
  private final StorageUrlResolver urls;

  S3StorageService(S3Client s3, @Value("${app.storage.s3.bucket}") String bucket, StorageUrlResolver urls) {
    this.s3 = s3;
    this.bucket = bucket;
    this.urls = urls;
  }

  @Override
  public StoredObject store(String key, String contentType, long contentLength, InputStream content) {
    validateKey(key);
    if (contentLength < 0) throw new IllegalArgumentException("Content length must not be negative");
    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType(contentType)
          .contentLength(contentLength)
          .cacheControl("public, max-age=31536000, immutable")
          .build();
      s3.putObject(request, RequestBody.fromInputStream(content, contentLength));
      return new StoredObject(key, urls.urlFor(key), contentType);
    } catch (S3Exception ex) {
      throw new IllegalStateException("Unable to store object in S3", ex);
    }
  }

  @Override
  public StoredContent load(String key) {
    validateKey(key);
    try {
      ResponseInputStream<GetObjectResponse> response = s3.getObject(GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build());
      GetObjectResponse metadata = response.response();
      String contentType = metadata.contentType() == null ? "application/octet-stream" : metadata.contentType();
      long contentLength = metadata.contentLength() == null ? -1 : metadata.contentLength();
      return new StoredContent(response, contentType, contentLength);
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) throw new ResourceNotFoundException("Image not found");
      throw new IllegalStateException("Unable to load object from S3", ex);
    }
  }

  @Override
  public void delete(String key) {
    validateKey(key);
    try {
      s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (S3Exception ex) {
      throw new IllegalStateException("Unable to delete object from S3", ex);
    }
  }

  private void validateKey(String key) {
    if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..")) {
      throw new IllegalArgumentException("Invalid storage key");
    }
  }
}
