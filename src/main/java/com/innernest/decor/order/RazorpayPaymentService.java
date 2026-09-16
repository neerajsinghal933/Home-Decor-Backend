package com.innernest.decor.order;

import com.innernest.decor.common.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayPaymentService {
  private final OrderService orders;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final String keyId;
  private final String keySecret;
  private final String currency;

  RazorpayPaymentService(
      OrderService orders,
      @Value("${app.razorpay.key-id:}") String keyId,
      @Value("${app.razorpay.key-secret:}") String keySecret,
      @Value("${app.razorpay.currency:INR}") String currency) {
    this.orders = orders;
    this.keyId = keyId;
    this.keySecret = keySecret;
    this.currency = currency;
  }

  @Transactional
  public RazorpayCreateOrderResponse create(String sessionId, CreateOrderRequest request) {
    ensureConfigured();
    Order local = orders.createPendingPaymentOrder(sessionId, request);
    int amountPaise = local.getTotal().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    String razorpayOrderId = createRazorpayOrder(local.getOrderNumber(), amountPaise);
    orders.attachRazorpayOrderId(local.getOrderNumber(), razorpayOrderId);
    return new RazorpayCreateOrderResponse(
        keyId,
        razorpayOrderId,
        local.getOrderNumber(),
        local.getTotal(),
        amountPaise,
        currency,
        local.getCustomerName(),
        local.getCustomerEmail(),
        local.getCustomerPhone());
  }

  @Transactional
  public OrderResponse verify(String sessionId, RazorpayVerifyRequest request) {
    ensureConfigured();
    String expected = hmac(request.razorpayOrderId() + "|" + request.razorpayPaymentId(), keySecret);
    if (!constantTimeEquals(expected, request.razorpaySignature())) {
      throw new BusinessRuleException("Payment signature verification failed");
    }
    return orders.markPaid(sessionId, request.localOrderNumber(), request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
  }

  private String createRazorpayOrder(String receipt, int amountPaise) {
    try {
      String body = "{\"amount\":" + amountPaise + ",\"currency\":\"" + currency + "\",\"receipt\":\"" + receipt + "\",\"payment_capture\":1}";
      String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
          .timeout(Duration.ofSeconds(15))
          .header("Authorization", "Basic " + auth)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BusinessRuleException("Unable to create Razorpay order");
      }
      java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
      if (!matcher.find()) throw new BusinessRuleException("Razorpay order response was invalid");
      return matcher.group(1);
    } catch (BusinessRuleException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessRuleException("Unable to connect to Razorpay");
    }
  }

  private void ensureConfigured() {
    if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
      throw new BusinessRuleException("Razorpay is not configured. Set app.razorpay.key-id and app.razorpay.key-secret.");
    }
  }

  private String hmac(String value, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) hex.append(String.format("%02x", b));
      return hex.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to verify payment signature", ex);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    return java.security.MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }
}
