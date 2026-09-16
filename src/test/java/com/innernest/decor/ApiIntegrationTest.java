package com.innernest.decor;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired
  MockMvc mvc;
  @Autowired
  JdbcTemplate jdbc;

  @Test
  void listsSeededCatalog() throws Exception {
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()", greaterThan(7)))
        .andExpect(jsonPath("$.items[0].name").value("Textured Ceramic Vase"))
        .andExpect(jsonPath("$.items[0].img").value("assets/crops/prod-vase.png"));
  }

  @Test
  void validatesNewsletterEmail() throws Exception {
    mvc.perform(post("/api/newsletter")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"bad\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"));
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
}
