package com.innernest.decor.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innernest.decor.common.BusinessRuleException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayOrdersClient {
  private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String keyId;
  private final String keySecret;

  @Autowired
  RazorpayOrdersClient(ObjectMapper objectMapper,
                       @Value("${app.razorpay.key-id:}") String keyId,
                       @Value("${app.razorpay.key-secret:}") String keySecret) {
    this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), objectMapper, keyId, keySecret);
  }

  RazorpayOrdersClient(HttpClient httpClient, ObjectMapper objectMapper, String keyId, String keySecret) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.keyId = keyId;
    this.keySecret = keySecret;
  }

  public String createOrder(String receipt, long amountPaise) {
    ensureConfigured();
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "amount", amountPaise,
          "currency", "INR",
          "receipt", receipt));
      String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      HttpRequest request = HttpRequest.newBuilder(ORDERS_URI)
          .timeout(Duration.ofSeconds(15))
          .header("Authorization", "Basic " + auth)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessRuleException("Unable to create a secure payment order. Please try again.");
      }
      JsonNode json = objectMapper.readTree(response.body());
      String id = json.path("id").asText("");
      if (id.isBlank()) throw new BusinessRuleException("Payment provider returned an invalid response");
      return id;
    } catch (BusinessRuleException ex) {
      throw ex;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BusinessRuleException("Payment provider is temporarily unavailable. Please try again.");
    } catch (Exception ex) {
      throw new BusinessRuleException("Payment provider is temporarily unavailable. Please try again.");
    }
  }

  public String publicKeyId() {
    ensureConfigured();
    return keyId;
  }

  private void ensureConfigured() {
    if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
      throw new BusinessRuleException("Online payment is temporarily unavailable");
    }
  }
}
