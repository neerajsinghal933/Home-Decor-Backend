package com.innernest.decor;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.innernest.decor.order.RazorpayOrdersClient;
import com.innernest.decor.order.RazorpayRefundResult;
import com.innernest.decor.order.RazorpayRefundsClient;
import com.innernest.decor.common.BusinessRuleException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired
  MockMvc mvc;
  @Autowired
  JdbcTemplate jdbc;
  @MockitoBean
  RazorpayOrdersClient razorpayOrdersClient;
  @MockitoBean
  RazorpayRefundsClient razorpayRefundsClient;

  @BeforeEach
  void configureRazorpayTestClient() {
    when(razorpayOrdersClient.publicKeyId()).thenReturn("test_public_key_id");
    when(razorpayOrdersClient.createOrder(anyString(), anyLong())).thenAnswer(invocation -> "order_test_" + invocation.getArgument(0));
    when(razorpayRefundsClient.createNormalRefund(anyString(), anyLong(), anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> new RazorpayRefundResult("rfnd_" + invocation.getArgument(2), invocation.getArgument(0),
            invocation.getArgument(1), "INR", "pending", null));
  }

  @Test
  void customerCanRequestOwnCancellationButNotAnotherCustomersOrInvalidState() throws Exception {
    PaidOrder owner = createPaidOrder("cancel-owner", "cancel-owner@example.com", 1);
    String other = googleToken("cancel-other", "cancel-other@example.com", "Other Customer");

    mvc.perform(post("/api/orders/my/" + owner.orderNumber() + "/cancellation")
            .header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Changed my mind\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(post("/api/orders/my/" + owner.orderNumber() + "/cancellation")
            .header("Authorization", "Bearer " + owner.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Changed my mind\",\"details\":\"No longer required\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order.status").value("CANCELLATION_REQUESTED"))
        .andExpect(jsonPath("$.request.status").value("REQUESTED"));
    mvc.perform(post("/api/orders/my/" + owner.orderNumber() + "/cancellation")
            .header("Authorization", "Bearer " + owner.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Duplicate\"}"))
        .andExpect(status().isBadRequest());
    assertEquals(1, jdbc.queryForObject("select count(*) from order_service_requests where order_id = (select id from orders where order_number = ?)", Integer.class, owner.orderNumber()));
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, owner.orderNumber());
    assertEquals(1, jdbc.queryForObject("select count(*) from transactional_email_events where event_key = ?", Integer.class, "CANCELLATION_REQUESTED:" + requestId));
  }

  @Test
  void deliveredOrderCanRequestReturnAndDuplicateOrIneligibleReturnIsRejected() throws Exception {
    PaidOrder eligible = createPaidOrder("return-owner", "return-owner@example.com", 2);
    PaidOrder ineligible = createPaidOrder("return-ineligible", "return-ineligible@example.com", 3);
    jdbc.update("update orders set status = 'DELIVERED' where order_number = ?", eligible.orderNumber());

    mvc.perform(post("/api/orders/my/" + ineligible.orderNumber() + "/return")
            .header("Authorization", "Bearer " + ineligible.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Damaged\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/orders/my/" + eligible.orderNumber() + "/return")
            .header("Authorization", "Bearer " + eligible.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Damaged\",\"details\":\"Cracked on arrival\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order.status").value("RETURN_REQUESTED"));
    mvc.perform(post("/api/orders/my/" + eligible.orderNumber() + "/return")
            .header("Authorization", "Bearer " + eligible.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Duplicate\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminApprovalCreatesOneServerCalculatedFullRefundAndRejectsNonAdmin() throws Exception {
    PaidOrder order = createPaidOrder("approve-owner", "approve-owner@example.com", 4);
    mvc.perform(post("/api/orders/my/" + order.orderNumber() + "/cancellation")
            .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Ordered by mistake\"}"))
        .andExpect(status().isOk());
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, order.orderNumber());

    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
            .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    String admin = adminToken("refund-admin", "refund-admin@example.com");
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
            .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"Approved before shipping\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund.status").value("PENDING"))
        .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
        .andExpect(jsonPath("$.paymentStatus").value("REFUND_PENDING"));
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
            .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());

    long totalPaise = jdbc.queryForObject("select cast(total * 100 as bigint) from orders where order_number = ?", Long.class, order.orderNumber());
    verify(razorpayRefundsClient, times(1)).createNormalRefund(order.paymentId(), totalPaise,
        "REF-" + order.orderNumber() + "-" + requestId, order.orderNumber(),
        jdbc.queryForObject("select idempotency_key from refund_transactions where service_request_id = ?", String.class, requestId));
    assertEquals(1, jdbc.queryForObject("select count(*) from refund_transactions where service_request_id = ?", Integer.class, requestId));
  }

  @Test
  void adminCanRejectRequestWithReasonAndNoRefund() throws Exception {
    PaidOrder order = createPaidOrder("reject-owner", "reject-owner@example.com", 5);
    mvc.perform(post("/api/orders/my/" + order.orderNumber() + "/cancellation")
            .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Changed mind\"}"))
        .andExpect(status().isOk());
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, order.orderNumber());
    String admin = adminToken("reject-admin", "reject-admin@example.com");
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/reject")
            .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/reject")
            .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"The parcel has entered dispatch processing.\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
    assertEquals("PLACED", jdbc.queryForObject("select status from orders where order_number = ?", String.class, order.orderNumber()));
    assertEquals(0, jdbc.queryForObject("select count(*) from refund_transactions where service_request_id = ?", Integer.class, requestId));
  }

  @Test
  void refundApiFailureIsPersistedWithoutRollingBackCancellation() throws Exception {
    PaidOrder order = createPaidOrder("refund-fail", "refund-fail@example.com", 6);
    mvc.perform(post("/api/orders/my/" + order.orderNumber() + "/cancellation")
        .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"No longer needed\"}")).andExpect(status().isOk());
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, order.orderNumber());
    when(razorpayRefundsClient.createNormalRefund(anyString(), anyLong(), anyString(), anyString(), anyString()))
        .thenThrow(new BusinessRuleException("gateway detail that must not leak"));
    String admin = adminToken("refund-fail-admin", "refund-fail-admin@example.com");
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.refund.status").value("FAILED"));
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.refund.status").value("FAILED"));
    assertEquals("CANCELLED", jdbc.queryForObject("select status from orders where order_number = ?", String.class, order.orderNumber()));
    assertEquals("REFUND_FAILED", jdbc.queryForObject("select payment_status from orders where order_number = ?", String.class, order.orderNumber()));
    assertEquals(1, jdbc.queryForObject("select count(*) from refund_transactions where service_request_id = ?", Integer.class, requestId));
    String key = jdbc.queryForObject("select idempotency_key from refund_transactions where service_request_id = ?", String.class, requestId);
    verify(razorpayRefundsClient, times(2)).createNormalRefund(anyString(), anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(key));
  }

  @Test
  void signedRefundFailedWebhookMarksRefundNeedsAttention() throws Exception {
    PaidOrder order = createPaidOrder("refund-hook-fail", "refund-hook-fail@example.com", 8);
    mvc.perform(post("/api/orders/my/" + order.orderNumber() + "/cancellation")
        .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"No longer needed\"}")).andExpect(status().isOk());
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, order.orderNumber());
    String admin = adminToken("hook-fail-admin", "hook-fail-admin@example.com");
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    String refundId = jdbc.queryForObject("select razorpay_refund_id from refund_transactions where service_request_id = ?", String.class, requestId);
    long amount = jdbc.queryForObject("select cast(amount * 100 as bigint) from refund_transactions where service_request_id = ?", Long.class, requestId);
    String body = refundWebhook("refund.failed", refundId, order.paymentId(), amount, "failed");
    mvc.perform(post("/api/payments/razorpay/webhook").header("X-Razorpay-Event-Id", "refund-failed-event")
        .header("X-Razorpay-Signature", hmac(body, "test_webhook_signature_secret")).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNoContent());
    assertEquals("FAILED", jdbc.queryForObject("select status from refund_transactions where razorpay_refund_id = ?", String.class, refundId));
    assertEquals("REFUND_FAILED", jdbc.queryForObject("select payment_status from orders where order_number = ?", String.class, order.orderNumber()));
  }

  @Test
  void signedRefundWebhookCompletesOnceAndInvalidUnknownOrDuplicateDeliveryIsSafe() throws Exception {
    PaidOrder order = createPaidOrder("refund-hook", "refund-hook@example.com", 7);
    mvc.perform(post("/api/orders/my/" + order.orderNumber() + "/cancellation")
        .header("Authorization", "Bearer " + order.token()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\":\"No longer needed\"}")).andExpect(status().isOk());
    Long requestId = jdbc.queryForObject("select id from order_service_requests where order_id = (select id from orders where order_number = ?)", Long.class, order.orderNumber());
    String admin = adminToken("hook-admin", "hook-admin@example.com");
    mvc.perform(post("/api/admin/order-requests/" + requestId + "/approve")
        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    String refundId = jdbc.queryForObject("select razorpay_refund_id from refund_transactions where service_request_id = ?", String.class, requestId);
    long amount = jdbc.queryForObject("select cast(amount * 100 as bigint) from refund_transactions where service_request_id = ?", Long.class, requestId);
    String body = refundWebhook("refund.processed", refundId, order.paymentId(), amount, "processed");

    mvc.perform(post("/api/payments/razorpay/webhook").header("X-Razorpay-Event-Id", "refund-invalid-signature")
        .header("X-Razorpay-Signature", "invalid").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    String unknown = refundWebhook("refund.processed", "rfnd_unknown", order.paymentId(), amount, "processed");
    mvc.perform(post("/api/payments/razorpay/webhook").header("X-Razorpay-Event-Id", "refund-unknown")
        .header("X-Razorpay-Signature", hmac(unknown, "test_webhook_signature_secret")).contentType(MediaType.APPLICATION_JSON).content(unknown))
        .andExpect(status().isNoContent());
    for (int i = 0; i < 2; i++) {
      mvc.perform(post("/api/payments/razorpay/webhook").header("X-Razorpay-Event-Id", "refund-processed-once")
          .header("X-Razorpay-Signature", hmac(body, "test_webhook_signature_secret")).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isNoContent());
    }
    assertEquals("PROCESSED", jdbc.queryForObject("select status from refund_transactions where razorpay_refund_id = ?", String.class, refundId));
    assertEquals("REFUNDED", jdbc.queryForObject("select payment_status from orders where order_number = ?", String.class, order.orderNumber()));
    assertEquals(1, jdbc.queryForObject("select count(*) from transactional_email_events where event_key = ?", Integer.class, "REFUND_PROCESSED:" + jdbc.queryForObject("select id from refund_transactions where razorpay_refund_id = ?", Long.class, refundId)));
  }

  @Test
  void razorpayEndpointsRequireAuthentication() throws Exception {
    mvc.perform(post("/api/payments/razorpay/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(onlineOrderJson("unauthenticated@example.com")))
        .andExpect(status().isUnauthorized());

    mvc.perform(post("/api/payments/razorpay/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"razorpayOrderId\":\"order_unknown\",\"razorpayPaymentId\":\"pay_unknown\",\"razorpaySignature\":\"invalid\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createsRazorpayOrderFromServerTotalAndReusesRepeatedRequest() throws Exception {
    String token = googleToken("payment-create-user", "payment-create@example.com", "Payment Create User");
    addAuthenticatedCartItem(token, 1, 1);

    String first = mvc.perform(post("/api/payments/razorpay/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(onlineOrderJson("payment-create@example.com").replace("{", "{\"amount\":1,")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amountPaise").value(269900))
        .andExpect(jsonPath("$.currency").value("INR"))
        .andExpect(jsonPath("$.keyId").value("test_public_key_id"))
        .andExpect(jsonPath("$.keySecret").doesNotExist())
        .andReturn().getResponse().getContentAsString();
    String razorpayOrderId = jsonString(first, "razorpayOrderId");

    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "payment-create@example.com"));
    assertEquals("PENDING", jdbc.queryForObject("select status from razorpay_payment_attempts where razorpay_order_id = ?", String.class, razorpayOrderId));
    mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    mvc.perform(post("/api/payments/razorpay/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(onlineOrderJson("payment-create@example.com")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.razorpayOrderId").value(razorpayOrderId));

    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "payment-create@example.com"));
    assertEquals(1, jdbc.queryForObject("select count(*) from razorpay_payment_attempts where customer_email = ?", Integer.class, "payment-create@example.com"));
    assertEquals(1, jdbc.queryForObject("select count(*) from cart_items ci join carts c on c.id = ci.cart_id join users u on u.id = c.user_id where u.email = ?", Integer.class, "payment-create@example.com"));
  }

  @Test
  void verifiesValidSignatureIdempotentlyAndRejectsOtherUsers() throws Exception {
    String ownerToken = googleToken("payment-owner", "payment-owner@example.com", "Payment Owner");
    String attackerToken = googleToken("payment-attacker", "payment-attacker@example.com", "Payment Attacker");
    addAuthenticatedCartItem(ownerToken, 2, 1);
    String created = createRazorpayOrder(ownerToken, "payment-owner@example.com");
    String orderId = jsonString(created, "razorpayOrderId");
    String paymentId = "pay_valid_owner";
    String signature = hmac(orderId + "|" + paymentId, "test_payment_signature_secret");
    String verification = verificationJson(orderId, paymentId, signature);

    mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + attackerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(verification))
        .andExpect(status().isNotFound());

    int stockBefore = jdbc.queryForObject("select stock from products where id = 2", Integer.class);
    mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(verification))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentStatus").value("Paid"))
        .andExpect(jsonPath("$.status").value("PLACED"));
    mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(verification))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentStatus").value("Paid"));

    assertEquals(stockBefore - 1, jdbc.queryForObject("select stock from products where id = 2", Integer.class));
    assertEquals("RAZORPAY", jdbc.queryForObject("select payment_provider from orders where razorpay_order_id = ?", String.class, orderId));
    assertEquals(1, jdbc.queryForObject("select count(*) from orders where razorpay_order_id = ? and paid_at is not null", Integer.class, orderId));
    assertEquals(0, jdbc.queryForObject("select count(*) from cart_items ci join carts c on c.id = ci.cart_id join users u on u.id = c.user_id where u.email = ?", Integer.class, "payment-owner@example.com"));
    assertEquals(1, jdbc.queryForObject("select count(*) from transactional_email_events where event_key like 'ORDER_CONFIRMED:%' and recipient = ?", Integer.class, "payment-owner@example.com"));
    mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PLACED"));
  }

  @Test
  void invalidOrMismatchedPaymentSignatureNeverMarksOrderPaid() throws Exception {
    String token = googleToken("payment-invalid", "payment-invalid@example.com", "Payment Invalid");
    addAuthenticatedCartItem(token, 3, 1);
    String created = createRazorpayOrder(token, "payment-invalid@example.com");
    String orderId = jsonString(created, "razorpayOrderId");

    mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(verificationJson(orderId, "pay_invalid", "not-a-valid-signature")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment verification failed"));
    mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(verificationJson("order_unrelated", "pay_invalid", hmac("order_unrelated|pay_invalid", "test_payment_signature_secret"))))
        .andExpect(status().isNotFound());

    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "payment-invalid@example.com"));
    assertEquals("PENDING", jdbc.queryForObject("select status from razorpay_payment_attempts where razorpay_order_id = ?", String.class, orderId));
    assertEquals(1, jdbc.queryForObject("select count(*) from cart_items ci join carts c on c.id = ci.cart_id join users u on u.id = c.user_id where u.email = ?", Integer.class, "payment-invalid@example.com"));
    mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

  }

  @Test
  void validatesWebhookRawBodyAndHandlesDuplicateDeliveryOnce() throws Exception {
    String token = googleToken("webhook-user", "webhook-user@example.com", "Webhook User");
    addAuthenticatedCartItem(token, 4, 1);
    String created = createRazorpayOrder(token, "webhook-user@example.com");
    String orderId = jsonString(created, "razorpayOrderId");
    String body = paymentCapturedWebhook(orderId, "pay_webhook", 194300);
    String signature = hmac(body, "test_webhook_signature_secret");
    int stockBefore = jdbc.queryForObject("select stock from products where id = 4", Integer.class);

    for (int delivery = 0; delivery < 2; delivery++) {
      mvc.perform(post("/api/payments/razorpay/webhook")
              .header("X-Razorpay-Event-Id", "event_webhook_duplicate")
              .header("X-Razorpay-Signature", signature)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNoContent());
    }

    assertEquals("PAID", jdbc.queryForObject("select payment_status from orders where razorpay_order_id = ?", String.class, orderId));
    assertEquals("PLACED", jdbc.queryForObject("select status from orders where razorpay_order_id = ?", String.class, orderId));
    assertEquals(stockBefore - 1, jdbc.queryForObject("select stock from products where id = 4", Integer.class));
    assertEquals(1, jdbc.queryForObject("select count(*) from razorpay_webhook_events where event_id = ?", Integer.class, "event_webhook_duplicate"));
  }

  @Test
  void failedPaymentWebhookNeverPlacesOrListsOrderAndIsIdempotent() throws Exception {
    String token = googleToken("webhook-failed-user", "webhook-failed@example.com", "Webhook Failed User");
    addAuthenticatedCartItem(token, 5, 1);
    String created = createRazorpayOrder(token, "webhook-failed@example.com");
    String orderId = jsonString(created, "razorpayOrderId");
    String body = paymentFailedWebhook(orderId, "pay_webhook_failed", 464300);
    String signature = hmac(body, "test_webhook_signature_secret");

    for (int delivery = 0; delivery < 2; delivery++) {
      mvc.perform(post("/api/payments/razorpay/webhook")
              .header("X-Razorpay-Event-Id", "event_webhook_failed_duplicate")
              .header("X-Razorpay-Signature", signature)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNoContent());
    }

    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "webhook-failed@example.com"));
    assertEquals("FAILED", jdbc.queryForObject("select status from razorpay_payment_attempts where razorpay_order_id = ?", String.class, orderId));
    assertEquals(1, jdbc.queryForObject("select count(*) from cart_items ci join carts c on c.id = ci.cart_id join users u on u.id = c.user_id where u.email = ?", Integer.class, "webhook-failed@example.com"));
    assertEquals(1, jdbc.queryForObject("select count(*) from razorpay_webhook_events where event_id = ?", Integer.class, "event_webhook_failed_duplicate"));
    assertEquals(1, jdbc.queryForObject("select count(*) from transactional_email_events where event_key like 'PAYMENT_FAILED:%' and recipient = ?", Integer.class, "webhook-failed@example.com"));
    mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    String retried = createRazorpayOrder(token, "webhook-failed@example.com");
    assertNotEquals(orderId, jsonString(retried, "razorpayOrderId"));
    assertEquals(2, jdbc.queryForObject("select count(*) from razorpay_payment_attempts where customer_email = ?", Integer.class, "webhook-failed@example.com"));
    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "webhook-failed@example.com"));

    jdbc.update("update users set role = 'ADMIN' where email = ?", "webhook-failed@example.com");
    String adminToken = googleToken("webhook-failed-user", "webhook-failed@example.com", "Webhook Failed User");
    String adminOrders = mvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertFalse(adminOrders.contains("webhook-failed@example.com"));
  }

  @Test
  void invalidWebhookSignatureDoesNotChangePaymentOrCart() throws Exception {
    String token = googleToken("webhook-invalid", "webhook-invalid@example.com", "Webhook Invalid");
    addAuthenticatedCartItem(token, 5, 1);
    String created = createRazorpayOrder(token, "webhook-invalid@example.com");
    String orderId = jsonString(created, "razorpayOrderId");
    String body = paymentCapturedWebhook(orderId, "pay_webhook_invalid", 464300);

    mvc.perform(post("/api/payments/razorpay/webhook")
            .header("X-Razorpay-Event-Id", "event_webhook_invalid")
            .header("X-Razorpay-Signature", "invalid")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    assertEquals(0, jdbc.queryForObject("select count(*) from orders where customer_email = ?", Integer.class, "webhook-invalid@example.com"));
    assertEquals("PENDING", jdbc.queryForObject("select status from razorpay_payment_attempts where razorpay_order_id = ?", String.class, orderId));
    assertEquals(1, jdbc.queryForObject("select count(*) from cart_items ci join carts c on c.id = ci.cart_id join users u on u.id = c.user_id where u.email = ?", Integer.class, "webhook-invalid@example.com"));
  }

  @Test
  void listsSeededCatalog() throws Exception {
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()", greaterThan(7)))
        .andExpect(jsonPath("$.items[0].name").value("Textured Ceramic Vase"))
        .andExpect(jsonPath("$.items[0].img").value("assets/crops/prod-vase.png"));
  }

  @Test
  void newsletterRequiresLoginUsesAccountEmailAndIsVisibleToAdmin() throws Exception {
    mvc.perform(post("/api/newsletter")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized());

    String subscriberToken = googleToken("newsletter-user", "newsletter@example.com", "Newsletter User");
    mvc.perform(post("/api/newsletter")
            .header("Authorization", "Bearer " + subscriberToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Subscribed"));

    String adminToken = googleToken("newsletter-admin", "newsletter-admin@example.com", "Newsletter Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "newsletter-admin@example.com");
    adminToken = googleToken("newsletter-admin", "newsletter-admin@example.com", "Newsletter Admin");
    mvc.perform(get("/api/admin/subscribers").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].email", hasItem("newsletter@example.com")))
        .andExpect(jsonPath("$[0].subscribedAt", notNullValue()));
  }

  @Test
  void contactRequiresLoginAndUsesAuthenticatedIdentity() throws Exception {
    mvc.perform(post("/api/contact")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"Please help\"}"))
        .andExpect(status().isUnauthorized());

    String token = googleToken("contact-user", "contact@example.com", "Contact User");
    mvc.perform(post("/api/contact")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Spoofed\",\"email\":\"spoofed@example.com\",\"message\":\"Please help with my order\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Message received"));

    assertEquals("Contact User", jdbc.queryForObject("select name from contact_messages where email = ?", String.class, "contact@example.com"));
  }

  @Test
  void googleLoginCreatesUserWithDefaultRoleAndMeWorks() throws Exception {
    String token = googleToken("google-user-1", "neeraj@example.com", "Neeraj");

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("neeraj@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));

    mvc.perform(post("/api/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"credential\":\"not-a-google-token\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void profileImageUploadUsesBackendUrlAndRemovesReplacedObject() throws Exception {
    String token = googleToken("profile-image-user", "profile-image@example.com", "Profile Image User");
    byte[] firstPng = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
    byte[] secondPng = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 2};

    String firstResponse = mvc.perform(multipart("/api/profile/image")
            .file(new MockMultipartFile("file", "first.png", "image/png", firstPng))
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.profileImageUrl").value(org.hamcrest.Matchers.startsWith("http://localhost/uploads/profile-images/")))
        .andReturn().getResponse().getContentAsString();
    String firstUrl = firstResponse.split("\"profileImageUrl\":\"")[1].split("\"")[0];

    mvc.perform(multipart("/api/profile/image")
            .file(new MockMultipartFile("file", "second.png", "image/png", secondPng))
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.profileImageUrl").value(org.hamcrest.Matchers.startsWith("http://localhost/uploads/profile-images/")));

    mvc.perform(get(firstUrl)).andExpect(status().isNotFound());
  }

  @Test
  void wishlistRequiresAuthAndPersistsForCurrentUser() throws Exception {
    mvc.perform(get("/api/wishlist"))
        .andExpect(status().isUnauthorized());

    String token = googleToken("wishlist-user-1", "wish@example.com", "Wish User");
    mvc.perform(post("/api/wishlist/items/1").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(1));

    mvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));

    mvc.perform(delete("/api/wishlist/items/1").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void adminPromotionIsSqlOnlyAndAdminApisPersist() throws Exception {
    String userToken = googleToken("admin-user-1", "admin@example.com", "Admin User");

    mvc.perform(post("/api/admin/products")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(adminProductJson("IND-QA-001", "qa-admin-vase", "QA Admin Vase", 1888)))
        .andExpect(status().isForbidden());

    jdbc.update("update users set role = 'ADMIN' where email = ?", "admin@example.com");
    String adminToken = googleToken("admin-user-1", "admin@example.com", "Admin User");

    mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalProducts", greaterThan(7)));

    String productId = mvc.perform(post("/api/admin/products")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(adminProductJson("IND-QA-001", "qa-admin-vase", "QA Admin Vase", 1888)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("QA Admin Vase"))
        .andReturn().getResponse().getContentAsString().split("\"id\":")[1].split(",")[0];

    mvc.perform(get("/api/products/slug/qa-admin-vase"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(1888.0));

    mvc.perform(put("/api/admin/products/" + productId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(adminProductJson("IND-QA-001", "qa-admin-vase", "QA Admin Vase Updated", 1999)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("QA Admin Vase Updated"));

    mvc.perform(delete("/api/admin/products/" + productId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    mvc.perform(delete("/api/admin/products/" + productId + "/permanent")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/products/slug/qa-admin-vase"))
        .andExpect(status().isNotFound());
  }

  @Test
  void managedTagsAreSeededValidatedAndCatalogCanFilterByTag() throws Exception {
    String token = googleToken("tag-admin-1", "tags@example.com", "Tag Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "tags@example.com");
    token = googleToken("tag-admin-1", "tags@example.com", "Tag Admin");
    mvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").exists());
    String tagId = mvc.perform(post("/api/admin/tags").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Seasonal Edit\",\"active\":true}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString().split("\"id\":")[1].split(",")[0];
    mvc.perform(post("/api/admin/tags").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" seasonal edit \",\"active\":true}"))
        .andExpect(status().isConflict());
    String productJson = adminProductJson("TAG-QA-001", "tagged-vase", "Tagged Vase", 1888).replace("\"image\":\"assets/crops/prod-vase.png\"", "\"image\":\"assets/crops/prod-vase.png\",\"tagIds\":[" + tagId + "]");
    mvc.perform(post("/api/admin/products").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(productJson))
        .andExpect(status().isOk()).andExpect(jsonPath("$.tags[0].name").value("Seasonal Edit"));
    mvc.perform(get("/api/products").param("tag", tagId).param("size", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].sku").value("TAG-QA-001"));
    mvc.perform(delete("/api/admin/tags/" + tagId).header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
  }

  @Test
  void permanentProductDeletionClearsCartsAndPreservesOrderHistory() throws Exception {
    String adminToken = googleToken("delete-admin", "delete-admin@example.com", "Delete Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "delete-admin@example.com");
    adminToken = googleToken("delete-admin", "delete-admin@example.com", "Delete Admin");
    String shopperToken = googleToken("delete-shopper", "delete-shopper@example.com", "Delete Shopper");

    String productResponse = mvc.perform(post("/api/admin/products")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(adminProductJson("DELETE-QA-001", "delete-qa-vase", "Delete QA Vase", 999)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    String productId = productResponse.split("\"id\":")[1].split(",")[0];

    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + shopperToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());
    String orderNumber = createOrder(null, shopperToken, "delete-shopper@example.com");

    mvc.perform(delete("/api/admin/products/" + productId + "/permanent")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/orders/" + orderNumber).header("Authorization", "Bearer " + shopperToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Delete QA Vase"));
    mvc.perform(get("/api/products/slug/delete-qa-vase")).andExpect(status().isNotFound());
  }

  @Test
  void userOrderHistoryAndAdminStatusUpdatesWork() throws Exception {
    String token = googleToken("order-user-1", "orders@example.com", "Order User");
    String adminToken = googleToken("order-admin-1", "orderadmin@example.com", "Order Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "orderadmin@example.com");
    adminToken = googleToken("order-admin-1", "orderadmin@example.com", "Order Admin");

    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());

    String orderNumber = mvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName":"Order User",
                  "phone":"9876543210",
                  "email":"orders@example.com",
                  "address":"24 Palm Grove",
                  "city":"Mumbai",
                  "state":"Maharashtra",
                  "pincode":"400050",
                  "paymentMethod":"Cash on Delivery",
                  "deliveryMethod":"Standard Delivery"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];

    mvc.perform(get("/api/orders/my").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(orderNumber));

    mvc.perform(put("/api/admin/orders/" + orderNumber + "/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PROCESSING\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROCESSING"));
  }

  @Test
  void createsOrderFromPersistentCartAndClearsIt() throws Exception {
    String session = "cart-test-session";

    mvc.perform(delete("/api/cart").header("X-Session-Id", session))
        .andExpect(status().isOk());

    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"qty\":2,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totals.itemCount").value(2));

    mvc.perform(post("/api/orders")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName":"Arun Tailor",
                  "phone":"9876543210",
                  "email":"arun.tailor@example.com",
                  "address":"24 Palm Grove",
                  "city":"Mumbai",
                  "state":"Maharashtra",
                  "pincode":"400050",
                  "paymentMethod":"Cash on Delivery",
                  "deliveryMethod":"Standard Delivery"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.paymentStatus").value("Pending"))
        .andExpect(jsonPath("$.items[0].name").value("Textured Ceramic Vase"))
        .andExpect(jsonPath("$.totals.itemCount").value(2));

    mvc.perform(get("/api/cart").header("X-Session-Id", session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void guestOrderDetailRequiresTheCreatingSession() throws Exception {
    String ownerSession = "guest-order-owner-session";
    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", ownerSession)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":3,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());

    String orderNumber = createOrder(ownerSession, null, "guest-owner@example.com");

    mvc.perform(get("/api/orders/" + orderNumber))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/orders/" + orderNumber).header("X-Session-Id", "unrelated-session"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/orders/" + orderNumber).header("X-Session-Id", ownerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customer.email").value("guest-owner@example.com"));
  }

  @Test
  void authenticatedOrderDetailRequiresTheOwnerOrAnAdmin() throws Exception {
    String ownerToken = googleToken("detail-owner", "detail-owner@example.com", "Detail Owner");
    String attackerToken = googleToken("detail-attacker", "detail-attacker@example.com", "Detail Attacker");
    String adminToken = googleToken("detail-admin", "detail-admin@example.com", "Detail Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "detail-admin@example.com");
    adminToken = googleToken("detail-admin", "detail-admin@example.com", "Detail Admin");

    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":4,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());

    String orderNumber = createOrder(null, ownerToken, "detail-owner@example.com");

    mvc.perform(get("/api/orders/" + orderNumber))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/orders/" + orderNumber).header("Authorization", "Bearer " + attackerToken))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/orders/" + orderNumber).header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk());
    mvc.perform(get("/api/orders/" + orderNumber).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void updatesAndRemovesCartItem() throws Exception {
    String session = "cart-update-session";

    mvc.perform(delete("/api/cart").header("X-Session-Id", session))
        .andExpect(status().isOk());

    String addResponse = mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":2,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totals.itemCount").value(1))
        .andReturn().getResponse().getContentAsString();
    String itemId = addResponse.split("\"id\":")[1].split(",")[0];

    mvc.perform(patch("/api/cart/items/" + itemId)
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"qty\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].qty").value(3))
        .andExpect(jsonPath("$.totals.itemCount").value(3));

    mvc.perform(delete("/api/cart/items/" + itemId).header("X-Session-Id", session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0))
        .andExpect(jsonPath("$.totals.itemCount").value(0));
  }

  @Test
  void updatesAndRemovesOnlyTheSelectedCartVariant() throws Exception {
    String session = "cart-variant-session";
    String firstResponse = mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"qty\":1,\"size\":\"Small\",\"color\":\"Sand\"}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String firstItemId = firstResponse.split("\"id\":")[1].split(",")[0];

    String secondResponse = mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1,\"qty\":2,\"size\":\"Large\",\"color\":\"Blue\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.totals.itemCount").value(3))
        .andReturn().getResponse().getContentAsString();
    String secondItemId = secondResponse.split("\"id\":")[2].split(",")[0];

    mvc.perform(patch("/api/cart/items/" + secondItemId)
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"qty\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.totals.itemCount").value(4));

    mvc.perform(delete("/api/cart/items/" + firstItemId).header("X-Session-Id", session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(Long.valueOf(secondItemId)))
        .andExpect(jsonPath("$.items[0].productId").value(1))
        .andExpect(jsonPath("$.items[0].size").value("Large"))
        .andExpect(jsonPath("$.items[0].color").value("Blue"))
        .andExpect(jsonPath("$.items[0].qty").value(3));
  }

  @Test
  void adminSizePricesDriveCatalogCartAndRazorpayAmount() throws Exception {
    String admin = adminToken("size-price-admin", "size-price-admin@example.com");
    String productJson = adminProductJson("SIZE-QA-001", "size-price-wall-art", "Size Price Wall Art", 999)
        .replace("\"image\":\"assets/crops/prod-vase.png\"",
            "\"image\":\"assets/crops/prod-vase.png\",\"sizeVariants\":["
                + "{\"size\":\"Medium (16 x 20 in)\",\"price\":1299},"
                + "{\"size\":\"Large (20 x 25 in)\",\"price\":1899}]");
    String created = mvc.perform(post("/api/admin/products")
            .header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(productJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(1299))
        .andExpect(jsonPath("$.sizeVariants.length()").value(2))
        .andExpect(jsonPath("$.sizeVariants[1].size").value("Large (20 x 25 in)"))
        .andReturn().getResponse().getContentAsString();
    long productId = Long.parseLong(created.split("\"id\":")[1].split(",")[0]);
    mvc.perform(put("/api/admin/products/" + productId)
            .header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(productJson.replace("\"price\":1899", "\"price\":1999")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sizeVariants[1].price").value(1999));
    mvc.perform(get("/api/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sizeVariants.length()").value(2))
        .andExpect(jsonPath("$.sizeVariants[1].price").value(1999));

    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", "invalid-size-price-session")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"qty\":1,\"size\":\"Browser supplied fake size\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Please select an available size for Size Price Wall Art"));

    String customer = googleToken("size-price-shopper", "size-price-shopper@example.com", "Size Shopper");
    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + customer)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"qty\":1,\"size\":\"Large (20 x 25 in)\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items[0].price").value(1999))
        .andExpect(jsonPath("$.totals.subtotal").value(1999));

    mvc.perform(post("/api/payments/razorpay/orders")
            .header("Authorization", "Bearer " + customer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(onlineOrderJson("size-price-shopper@example.com")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amountPaise").value(215900));
  }

  @Test
  void concurrentOrderSubmissionsCreateExactlyOneOrderAndReturnAConflict() throws Exception {
    String session = "concurrent-order-session";
    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":6,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());
    String orderJson = """
        {
          "fullName":"Concurrent Buyer",
          "phone":"9876543210",
          "email":"concurrent-order@example.com",
          "address":"24 Palm Grove",
          "city":"Mumbai",
          "state":"Maharashtra",
          "pincode":"400050",
          "paymentMethod":"Cash on Delivery",
          "deliveryMethod":"Standard Delivery"
        }
        """;

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int attempt = 0; attempt < 2; attempt++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          start.await();
          return mvc.perform(post("/api/orders")
                  .header("X-Session-Id", session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(orderJson))
              .andReturn().getResponse().getStatus();
        }));
      }
      ready.await();
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> future : futures) statuses.add(future.get());
      Collections.sort(statuses);
      assertEquals(List.of(201, 409), statuses);
      assertEquals(1, jdbc.queryForObject(
          "select count(*) from orders where customer_email = ?",
          Integer.class,
          "concurrent-order@example.com"));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void directCheckoutCannotMarkOnlinePaymentPaidWithoutGatewayVerification() throws Exception {
    String session = "online-payment-bypass-session";
    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":7,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());

    mvc.perform(post("/api/orders")
            .header("X-Session-Id", session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fullName":"Gateway Bypass",
                  "phone":"9876543210",
                  "email":"gateway-bypass@example.com",
                  "address":"24 Palm Grove",
                  "city":"Mumbai",
                  "state":"Maharashtra",
                  "pincode":"400050",
                  "paymentMethod":"UPI",
                  "deliveryMethod":"Standard Delivery"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Online payments must be completed through Razorpay"));

    mvc.perform(get("/api/cart").header("X-Session-Id", session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totals.itemCount").value(1));
  }

  @Test
  void guestCartRequiresASessionButAuthenticatedCartDoesNot() throws Exception {
    mvc.perform(get("/api/cart"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Session ID is required for guest cart access"));
    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", "   ")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":5,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Session ID is required for guest cart access"));

    String token = googleToken("headerless-cart-user", "headerless-cart@example.com", "Headerless Cart User");
    mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":5,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items[0].productId").value(5));

    mvc.perform(post("/api/cart/items")
            .header("X-Session-Id", "x".repeat(500))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":5,\"qty\":1,\"size\":\"Medium\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Session ID must be 16-120 letters, numbers, dots, underscores, colons, or hyphens"));
  }

  @Test
  void duplicateAndMalformedAdminInputsReturnControlledClientErrors() throws Exception {
    String adminToken = googleToken("validation-admin", "validation-admin@example.com", "Validation Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", "validation-admin@example.com");
    adminToken = googleToken("validation-admin", "validation-admin@example.com", "Validation Admin");
    String promo = """
        {
          "code":"DUPQA20",
          "description":"Duplicate regression",
          "discountType":"PERCENT",
          "discountValue":20,
          "minimumOrderAmount":0,
          "active":true
        }
        """;

    mvc.perform(post("/api/admin/promos")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(promo))
        .andExpect(status().isOk());
    mvc.perform(post("/api/admin/promos")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(promo))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Promo code already exists"));
    mvc.perform(put("/api/admin/orders/DOES-NOT-EXIST/status")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"BOGUS\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));
  }

  @Test
  void customersCanSubmitFilterAndSortPersistedProductReviews() throws Exception {
    String admin = adminToken("review-admin", "review-admin@example.com");
    mvc.perform(post("/api/admin/products")
            .header("Authorization", "Bearer " + admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(adminProductJson("REVIEW-QA-001", "review-qa-product", "Review QA Product", 2499)))
        .andExpect(status().isOk());
    Long productId = jdbc.queryForObject("select id from products where sku = ?", Long.class, "REVIEW-QA-001");
    jdbc.update("update products set display_order = 9999 where id = ?", productId);

    PaidOrder verifiedBuyer = createPaidOrder("verified-reviewer", "verified-reviewer@example.com", productId);
    MockMultipartFile photo = new MockMultipartFile(
        "image", "room.png", "image/png",
        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00});
    mvc.perform(multipart("/api/products/" + productId + "/reviews")
            .file(photo)
            .header("Authorization", "Bearer " + verifiedBuyer.token())
            .param("rating", "5")
            .param("title", "Beautiful in our living room")
            .param("body", "The finish and proportions are excellent."))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rating").value(5))
        .andExpect(jsonPath("$.verifiedPurchase").value(true))
        .andExpect(jsonPath("$.imageUrl", notNullValue()));

    mvc.perform(multipart("/api/products/" + productId + "/reviews")
            .header("Authorization", "Bearer " + verifiedBuyer.token())
            .param("rating", "4").param("title", "Duplicate").param("body", "Second review"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("You have already reviewed this product"));

    String secondReviewer = googleToken("second-reviewer", "second-reviewer@example.com", "Second Reviewer");
    mvc.perform(multipart("/api/products/" + productId + "/reviews")
            .header("Authorization", "Bearer " + secondReviewer)
            .param("rating", "3")
            .param("title", "Good, with room to improve")
            .param("body", "The piece looks good and arrived safely."))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.verifiedPurchase").value(false));

    mvc.perform(get("/api/products/" + productId + "/reviews?sort=lowest&rating=3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.averageRating").value(4.0))
        .andExpect(jsonPath("$.totalReviews").value(2))
        .andExpect(jsonPath("$.breakdown[0].rating").value(5))
        .andExpect(jsonPath("$.breakdown[0].count").value(1))
        .andExpect(jsonPath("$.breakdown[2].rating").value(3))
        .andExpect(jsonPath("$.breakdown[2].count").value(1))
        .andExpect(jsonPath("$.reviews.length()").value(1))
        .andExpect(jsonPath("$.reviews[0].rating").value(3));

    mvc.perform(get("/api/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rating").value(4.0))
        .andExpect(jsonPath("$.reviews").value(2));

    mvc.perform(multipart("/api/products/" + productId + "/reviews")
            .param("rating", "5").param("title", "No auth").param("body", "Must sign in"))
        .andExpect(status().isUnauthorized());
  }

  private String googleToken(String subject, String email, String name) throws Exception {
    return mvc.perform(post("/api/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"credential\":\"test-google:" + subject + ":" + email + ":" + name + ":https://example.com/avatar.png\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", notNullValue()))
        .andReturn().getResponse().getContentAsString().split("\"token\":\"")[1].split("\"")[0];
  }

  private String createOrder(String sessionId, String token, String email) throws Exception {
    var request = post("/api/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "fullName":"Order Owner",
              "phone":"9876543210",
              "email":"%s",
              "address":"24 Palm Grove",
              "city":"Mumbai",
              "state":"Maharashtra",
              "pincode":"400050",
              "paymentMethod":"Cash on Delivery",
              "deliveryMethod":"Standard Delivery"
            }
            """.formatted(email));
    if (sessionId != null) request.header("X-Session-Id", sessionId);
    if (token != null) request.header("Authorization", "Bearer " + token);
    return mvc.perform(request)
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];
  }

  private void addAuthenticatedCartItem(String token, long productId, int qty) throws Exception {
    mvc.perform(post("/api/cart/items")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":" + productId + ",\"qty\":" + qty + ",\"size\":\"Medium\"}"))
        .andExpect(status().isCreated());
  }

  private String createRazorpayOrder(String token, String email) throws Exception {
    return mvc.perform(post("/api/payments/razorpay/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(onlineOrderJson(email)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
  }

  private PaidOrder createPaidOrder(String subject, String email, long productId) throws Exception {
    String token = googleToken(subject, email, "Lifecycle Customer");
    addAuthenticatedCartItem(token, productId, 1);
    String created = createRazorpayOrder(token, email);
    String razorpayOrderId = jsonString(created, "razorpayOrderId");
    String paymentId = "pay_" + subject.replace('-', '_');
    String signature = hmac(razorpayOrderId + "|" + paymentId, "test_payment_signature_secret");
    String verified = mvc.perform(post("/api/payments/razorpay/verify")
            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content(verificationJson(razorpayOrderId, paymentId, signature)))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return new PaidOrder(token, jsonString(verified, "id"), paymentId);
  }

  private String adminToken(String subject, String email) throws Exception {
    googleToken(subject, email, "Lifecycle Admin");
    jdbc.update("update users set role = 'ADMIN' where email = ?", email);
    return googleToken(subject, email, "Lifecycle Admin");
  }

  private String onlineOrderJson(String email) {
    return """
        {
          "fullName":"Online Buyer",
          "phone":"9876543210",
          "email":"%s",
          "address":"24 Palm Grove",
          "city":"Mumbai",
          "state":"Maharashtra",
          "pincode":"400050",
          "paymentMethod":"Razorpay - UPI",
          "deliveryMethod":"Standard Delivery"
        }
        """.formatted(email);
  }

  private String verificationJson(String orderId, String paymentId, String signature) {
    return "{\"razorpayOrderId\":\"" + orderId + "\",\"razorpayPaymentId\":\"" + paymentId
        + "\",\"razorpaySignature\":\"" + signature + "\"}";
  }

  private String paymentCapturedWebhook(String orderId, String paymentId, long amount) {
    return "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\""
        + paymentId + "\",\"order_id\":\"" + orderId + "\",\"amount\":" + amount
        + ",\"currency\":\"INR\",\"status\":\"captured\",\"captured\":true}}}}";
  }

  private String paymentFailedWebhook(String orderId, String paymentId, long amount) {
    return "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\""
        + paymentId + "\",\"order_id\":\"" + orderId + "\",\"amount\":" + amount
        + ",\"currency\":\"INR\",\"status\":\"failed\",\"captured\":false}}}}";
  }

  private String refundWebhook(String event, String refundId, String paymentId, long amount, String status) {
    return "{\"event\":\"" + event + "\",\"payload\":{\"refund\":{\"entity\":{\"id\":\""
        + refundId + "\",\"payment_id\":\"" + paymentId + "\",\"amount\":" + amount
        + ",\"currency\":\"INR\",\"status\":\"" + status + "\",\"acquirer_data\":{\"arn\":\"ARN123\"}}}}}";
  }

  private String jsonString(String json, String field) {
    return json.split("\"" + field + "\":\"")[1].split("\"")[0];
  }

  private String hmac(String value, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  private String adminProductJson(String sku, String slug, String name, int price) {
    return """
        {
          "sku":"%s",
          "slug":"%s",
          "name":"%s",
          "description":"A QA-created decor product.",
          "categoryId":"vases",
          "price":%d,
          "old":null,
          "badge":"New",
          "color":"Sand",
          "material":"Ceramic",
          "dimensions":"Height: 20 cm",
          "stock":6,
          "reviews":0,
          "featured":true,
          "active":true,
          "image":"assets/crops/prod-vase.png"
        }
        """.formatted(sku, slug, name, price);
  }

  private record PaidOrder(String token, String orderNumber, String paymentId) {}
}
