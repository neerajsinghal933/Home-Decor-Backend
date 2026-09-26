package com.innernest.decor.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innernest.decor.common.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayPaymentService {
  private static final String CURRENCY = "INR";

  private final RazorpayPaymentAttemptService attempts;
  private final RazorpayOrdersClient razorpayClient;
  private final RazorpayWebhookEventRepository webhookEvents;
  private final ObjectMapper objectMapper;
  private final OrderLifecycleService lifecycle;
  private final String keySecret;
  private final String webhookSecret;

  RazorpayPaymentService(
      RazorpayPaymentAttemptService attempts,
      RazorpayOrdersClient razorpayClient,
      RazorpayWebhookEventRepository webhookEvents,
      ObjectMapper objectMapper,
      OrderLifecycleService lifecycle,
      @Value("${app.razorpay.key-secret:}") String keySecret,
      @Value("${app.razorpay.webhook-secret:}") String webhookSecret) {
    this.attempts = attempts;
    this.razorpayClient = razorpayClient;
    this.webhookEvents = webhookEvents;
    this.objectMapper = objectMapper;
    this.lifecycle = lifecycle;
    this.keySecret = keySecret;
    this.webhookSecret = webhookSecret;
  }

  @Transactional
  public RazorpayCreateOrderResponse create(CreateOrderRequest request) {
    ensurePaymentVerificationConfigured();
    CreateOrderRequest onlineRequest = new CreateOrderRequest(
        request.fullName(), request.phone(), request.email(), request.address(), request.city(), request.state(),
        request.pincode(), request.landmark(), "Razorpay", request.deliveryMethod(), request.promoCode());
    RazorpayPaymentAttemptService.PendingAttempt pending = attempts.createOrReuse(onlineRequest);
    RazorpayPaymentAttempt attempt = pending.attempt();
    long amountPaise = toPaise(attempt.getTotal());
    if (!pending.reused()) {
      String razorpayOrderId = razorpayClient.createOrder(attempt.getReceipt(), amountPaise);
      attempts.attachRazorpayOrderId(attempt.getReceipt(), razorpayOrderId);
      attempt.setRazorpayOrderId(razorpayOrderId);
    }
    return new RazorpayCreateOrderResponse(
        razorpayClient.publicKeyId(),
        attempt.getRazorpayOrderId(),
        attempt.getReceipt(),
        amountPaise,
        CURRENCY,
        attempt.getCustomerName(),
        attempt.getCustomerEmail(),
        attempt.getCustomerPhone());
  }

  public OrderResponse verify(RazorpayVerifyRequest request) {
    ensurePaymentVerificationConfigured();
    RazorpayPaymentAttempt attempt = attempts.forCurrentUser(request.razorpayOrderId());
    String expected = hmac(attempt.getRazorpayOrderId() + "|" + request.razorpayPaymentId(), keySecret);
    if (!constantTimeEquals(expected, request.razorpaySignature())) {
      throw new BusinessRuleException("Payment verification failed");
    }
    return attempts.completeForCurrentUser(attempt.getRazorpayOrderId(), request.razorpayPaymentId());
  }

  @Transactional
  public void processWebhook(String eventId, String signature, byte[] rawBody) {
    ensureWebhookConfigured();
    if (eventId == null || eventId.isBlank() || eventId.length() > 120) {
      throw new BusinessRuleException("Invalid webhook event");
    }
    String expected = hmac(rawBody, webhookSecret);
    if (signature == null || !constantTimeEquals(expected, signature)) {
      throw new BusinessRuleException("Invalid webhook signature");
    }
    if (webhookEvents.existsByEventId(eventId)) return;

    try {
      JsonNode root = objectMapper.readTree(rawBody);
      String eventType = root.path("event").asText("");
      if (eventType.length() > 80) throw new BusinessRuleException("Invalid webhook event");
      if ("payment.captured".equals(eventType) || "payment.failed".equals(eventType)) {
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asText("");
        String paymentId = payment.path("id").asText("");
        long amount = payment.path("amount").asLong(-1);
        String currency = payment.path("currency").asText("");
        String paymentStatus = payment.path("status").asText("");
        boolean captured = payment.path("captured").asBoolean(false)
            || "captured".equals(payment.path("status").asText(""));
        boolean failed = "payment.failed".equals(eventType) && "failed".equals(paymentStatus);
        if (orderId.isBlank() || paymentId.isBlank() || amount < 0 || !CURRENCY.equals(currency)
            || (!captured && !failed)) {
          throw new BusinessRuleException("Invalid payment webhook payload");
        }
        RazorpayPaymentAttempt attempt = attempts.findForWebhook(orderId).orElse(null);
        if (attempt != null) {
          if (toPaise(attempt.getTotal()) != amount) {
            throw new BusinessRuleException("Payment amount mismatch");
          }
          if (captured) attempts.completeFromWebhook(orderId, paymentId);
          else attempts.markFailedFromWebhook(orderId);
        }
      } else if ("refund.created".equals(eventType) || "refund.processed".equals(eventType)
          || "refund.failed".equals(eventType)) {
        JsonNode refund = root.path("payload").path("refund").path("entity");
        String refundId = refund.path("id").asText("");
        String paymentId = refund.path("payment_id").asText("");
        String receipt = refund.path("receipt").asText("");
        long amount = refund.path("amount").asLong(-1);
        String currency = refund.path("currency").asText("");
        String gatewayStatus = refund.path("status").asText("");
        boolean statusMatches = ("refund.processed".equals(eventType) && "processed".equals(gatewayStatus))
            || ("refund.failed".equals(eventType) && "failed".equals(gatewayStatus))
            || "refund.created".equals(eventType);
        if (refundId.isBlank() || paymentId.isBlank() || amount < 100 || !CURRENCY.equals(currency) || !statusMatches) {
          throw new BusinessRuleException("Invalid refund webhook payload");
        }
        JsonNode acquirer = refund.path("acquirer_data");
        String reference = firstText(acquirer, "arn", "rrn", "utr");
        lifecycle.applyRefundWebhook(eventType, refundId, receipt, paymentId, amount, currency, reference);
      }
      webhookEvents.save(new RazorpayWebhookEvent(eventId, eventType.isBlank() ? "unknown" : eventType));
    } catch (BusinessRuleException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessRuleException("Invalid webhook payload");
    }
  }

  private String firstText(JsonNode node, String... names) {
    for (String name : names) {
      String value = node.path(name).asText("");
      if (!value.isBlank()) return value;
    }
    return null;
  }

  static long toPaise(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) throw new BusinessRuleException("Order total must be positive");
    try {
      return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    } catch (ArithmeticException ex) {
      throw new BusinessRuleException("Order total has an invalid precision");
    }
  }

  private void ensurePaymentVerificationConfigured() {
    if (keySecret == null || keySecret.isBlank()) {
      throw new BusinessRuleException("Online payment is temporarily unavailable");
    }
  }

  private void ensureWebhookConfigured() {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new BusinessRuleException("Payment webhook is not configured");
    }
  }

  private String hmac(String value, String secret) {
    return hmac(value.getBytes(StandardCharsets.UTF_8), secret);
  }

  private String hmac(byte[] value, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to verify payment signature", ex);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    if (left == null || right == null) return false;
    return java.security.MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }
}
