package com.innernest.decor.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innernest.decor.common.ResourceNotFoundException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3StorageServiceTest {
  @Mock
  private S3Client s3;
  private S3StorageService storage;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    storage = new S3StorageService(s3, "inner-nest-images", new StorageUrlResolver("https://api.innernestdecor.com"));
  }

  @Test
  void storesPrivateObjectAndReturnsBackendUrl() {
    byte[] bytes = "image".getBytes(StandardCharsets.UTF_8);
    when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    StoredObject result = storage.store(
        "product-images/id.webp", "image/webp", bytes.length, new ByteArrayInputStream(bytes));

    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3).putObject(request.capture(), any(RequestBody.class));
    assertEquals("inner-nest-images", request.getValue().bucket());
    assertEquals("product-images/id.webp", request.getValue().key());
    assertEquals("image/webp", request.getValue().contentType());
    assertNull(request.getValue().acl());
    assertEquals("https://api.innernestdecor.com/uploads/product-images/id.webp", result.url());
  }

  @Test
  void streamsObjectMetadataAndContent() throws Exception {
    byte[] bytes = "content".getBytes(StandardCharsets.UTF_8);
    ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
        GetObjectResponse.builder().contentType("image/png").contentLength((long) bytes.length).build(),
        AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    when(s3.getObject(any(GetObjectRequest.class))).thenReturn(response);

    try (StoredContent content = storage.load("profile-images/id.png")) {
      assertEquals("image/png", content.contentType());
      assertEquals(bytes.length, content.contentLength());
      assertEquals("content", new String(content.inputStream().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  void mapsMissingObjectToNotFound() {
    when(s3.getObject(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().statusCode(404).message("missing").build());

    assertThrows(ResourceNotFoundException.class, () -> storage.load("product-images/missing.png"));
  }

  @Test
  void deletesOnlyTheConfiguredBucketKey() {
    storage.delete("profile-images/old.jpg");

    ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3).deleteObject(request.capture());
    assertEquals("inner-nest-images", request.getValue().bucket());
    assertEquals("profile-images/old.jpg", request.getValue().key());
  }
}
