package com.innernest.decor.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innernest.decor.common.BusinessRuleException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayRefundsClient {
  private final HttpClient httpClient;
  private final ObjectMapper mapper;
  private final String keyId;
  private final String keySecret;

  @Autowired
  RazorpayRefundsClient(ObjectMapper mapper,
      @Value("${app.razorpay.key-id:}") String keyId,
      @Value("${app.razorpay.key-secret:}") String keySecret) {
    this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), mapper, keyId, keySecret);
  }

  RazorpayRefundsClient(HttpClient client, ObjectMapper mapper, String keyId, String keySecret) {
    this.httpClient = client; this.mapper = mapper; this.keyId = keyId; this.keySecret = keySecret;
  }

  public RazorpayRefundResult createNormalRefund(String paymentId, long amountPaise, String receipt,
                                                  String orderNumber, String idempotencyKey) {
    ensureConfigured();
    if (paymentId == null || !paymentId.startsWith("pay_")) throw new BusinessRuleException("Order payment reference is invalid");
    try {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("amount", amountPaise);
      body.put("speed", "normal");
      body.put("receipt", receipt);
      body.put("notes", Map.of("inner_nest_order", orderNumber));
      String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      URI uri = URI.create("https://api.razorpay.com/v1/payments/"
          + URLEncoder.encode(paymentId, StandardCharsets.UTF_8) + "/refund");
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
          .header("Authorization", "Basic " + auth)
          .header("Content-Type", "application/json")
          .header("X-Refund-Idempotency", idempotencyKey)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 409) throw new RefundInProgressException();
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessRuleException("The refund could not be initiated. It requires administrator attention.");
      }
      JsonNode json = mapper.readTree(response.body());
      String id = json.path("id").asText("");
      String returnedPayment = json.path("payment_id").asText("");
      long returnedAmount = json.path("amount").asLong(-1);
      String currency = json.path("currency").asText("");
      if (id.isBlank() || !paymentId.equals(returnedPayment) || returnedAmount != amountPaise || !"INR".equals(currency)) {
        throw new BusinessRuleException("The refund provider returned an invalid response. It requires administrator attention.");
      }
      JsonNode acquirer = json.path("acquirer_data");
      String reference = firstText(acquirer, "arn", "rrn", "utr");
      return new RazorpayRefundResult(id, returnedPayment, returnedAmount, currency,
          json.path("status").asText("pending"), reference);
    } catch (RefundInProgressException ex) {
      throw ex;
    } catch (BusinessRuleException ex) {
      throw ex;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BusinessRuleException("The refund provider is temporarily unavailable. It requires administrator attention.");
    } catch (Exception ex) {
      throw new BusinessRuleException("The refund provider is temporarily unavailable. It requires administrator attention.");
    }
  }

  private String firstText(JsonNode node, String... names) {
    for (String name : names) {
      String value = node.path(name).asText("");
      if (!value.isBlank()) return value;
    }
    return null;
  }

  private void ensureConfigured() {
    if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
      throw new BusinessRuleException("Refund processing is temporarily unavailable");
    }
  }
}
