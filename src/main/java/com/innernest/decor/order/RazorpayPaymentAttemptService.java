package com.innernest.decor.order;

import com.innernest.decor.cart.Cart;
import com.innernest.decor.cart.CartItem;
import com.innernest.decor.cart.CartService;
import com.innernest.decor.catalog.Product;
import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.DuplicateResourceException;
import com.innernest.decor.common.MoneyUtils;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.promo.PromoCodeService;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayPaymentAttemptService {
  private final RazorpayPaymentAttemptRepository attempts;
  private final OrderRepository orders;
  private final ProductRepository products;
  private final CartService carts;
  private final UserRepository users;
  private final PromoCodeService promos;
  private final OrderStatusHistoryRepository history;
  private final TransactionalOrderMailService mail;
  private final SecureRandom random = new SecureRandom();

  RazorpayPaymentAttemptService(RazorpayPaymentAttemptRepository attempts, OrderRepository orders,
                                ProductRepository products, CartService carts, UserRepository users,
                                PromoCodeService promos, OrderStatusHistoryRepository history,
                                TransactionalOrderMailService mail) {
    this.attempts = attempts;
    this.orders = orders;
    this.products = products;
    this.carts = carts;
    this.users = users;
    this.promos = promos;
    this.history = history;
    this.mail = mail;
  }

  @Transactional
  public PendingAttempt createOrReuse(CreateOrderRequest request) {
    Long userId = currentUserId();
    Cart cart = carts.loadForCheckout(null);
    RazorpayPaymentAttempt reusable = attempts
        .findFirstByUserIdAndStatusAndRazorpayOrderIdIsNotNullOrderByCreatedAtDesc(
            userId, RazorpayPaymentAttemptStatus.PENDING)
        .filter(attempt -> sameCheckout(attempt, cart, request))
        .orElse(null);
    if (reusable != null) return new PendingAttempt(reusable, true);
    if (cart.getItems().isEmpty()) {
      throw new DuplicateResourceException("Cart is empty or has already been checked out");
    }

    RazorpayPaymentAttempt attempt = new RazorpayPaymentAttempt();
    attempt.setReceipt(nextReceipt());
    attempt.setUser(users.getReferenceById(userId));
    attempt.setCustomerName(request.fullName());
    attempt.setCustomerPhone(request.phone());
    attempt.setCustomerEmail(request.email());
    attempt.setAddress(request.address());
    attempt.setCity(request.city());
    attempt.setState(request.state());
    attempt.setPincode(request.pincode());
    attempt.setLandmark(request.landmark());
    attempt.setDeliveryMethod(blankDefault(request.deliveryMethod(), "Standard Delivery"));
    attempt.setEstimatedDelivery(attempt.getDeliveryMethod().startsWith("Express")
        ? "1-2 working days" : "3-5 working days");

    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem cartItem : cart.getItems()) {
      Product product = cartItem.getProduct();
      if (product.getStock() < cartItem.getQty()) {
        throw new BusinessRuleException("Insufficient stock for " + product.getName());
      }
      BigDecimal unitPrice = carts.priceFor(cartItem);
      BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQty()));
      subtotal = subtotal.add(lineTotal);

      RazorpayPaymentAttemptItem item = new RazorpayPaymentAttemptItem();
      item.setAttempt(attempt);
      item.setProduct(product);
      item.setProductSku(product.getSku());
      item.setProductName(product.getName());
      item.setProductImage(product.getPrimaryImage());
      item.setColor(cartItem.getColor());
      item.setSize(cartItem.getSize());
      item.setUnitPrice(unitPrice);
      item.setQty(cartItem.getQty());
      item.setLineTotal(lineTotal);
      attempt.getItems().add(item);
    }

    BigDecimal shipping = deliveryFee(attempt.getDeliveryMethod(), subtotal);
    BigDecimal tax = MoneyUtils.tax(subtotal);
    BigDecimal discount = promos.discountFor(request.promoCode(), subtotal);
    attempt.setSubtotal(subtotal);
    attempt.setShipping(shipping);
    attempt.setEstimatedTax(tax);
    attempt.setDiscountAmount(discount);
    attempt.setPromoCode(normalizedPromo(request.promoCode()));
    attempt.setTotal(subtotal.add(shipping).add(tax).subtract(discount));
    return new PendingAttempt(attempts.save(attempt), false);
  }

  @Transactional
  public void attachRazorpayOrderId(String receipt, String razorpayOrderId) {
    RazorpayPaymentAttempt attempt = attempts.findByReceipt(receipt)
        .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found"));
    attempt.setRazorpayOrderId(razorpayOrderId);
  }

  @Transactional
  public RazorpayPaymentAttempt forCurrentUser(String razorpayOrderId) {
    RazorpayPaymentAttempt attempt = find(razorpayOrderId);
    if (!attempt.getUser().getId().equals(currentUserId())) {
      throw new ResourceNotFoundException("Payment attempt not found");
    }
    return attempt;
  }

  @Transactional
  public OrderResponse completeForCurrentUser(String razorpayOrderId, String razorpayPaymentId) {
    RazorpayPaymentAttempt attempt = forCurrentUser(razorpayOrderId);
    return complete(attempt, razorpayPaymentId);
  }

  @Transactional
  public Optional<RazorpayPaymentAttempt> findForWebhook(String razorpayOrderId) {
    return attempts.findWithItemsByRazorpayOrderId(razorpayOrderId);
  }

  @Transactional
  public void completeFromWebhook(String razorpayOrderId, String razorpayPaymentId) {
    attempts.findWithItemsByRazorpayOrderId(razorpayOrderId)
        .ifPresent(attempt -> complete(attempt, razorpayPaymentId));
  }

  @Transactional
  public void markFailedFromWebhook(String razorpayOrderId) {
    attempts.findWithItemsByRazorpayOrderId(razorpayOrderId).ifPresent(attempt -> {
      if (attempt.getStatus() == RazorpayPaymentAttemptStatus.PENDING) {
        attempt.setStatus(RazorpayPaymentAttemptStatus.FAILED);
        mail.paymentFailed(attempt);
      }
    });
  }

  private OrderResponse complete(RazorpayPaymentAttempt attempt, String razorpayPaymentId) {
    if (attempt.getStatus() == RazorpayPaymentAttemptStatus.COMPLETED) {
      if (!Objects.equals(attempt.getRazorpayPaymentId(), razorpayPaymentId)) {
        throw new BusinessRuleException("Payment attempt is already associated with another payment");
      }
      return OrderResponse.from(attempt.getCompletedOrder());
    }
    if (attempt.getStatus() != RazorpayPaymentAttemptStatus.PENDING
        && attempt.getStatus() != RazorpayPaymentAttemptStatus.FAILED) {
      throw new BusinessRuleException("Payment attempt cannot be completed");
    }

    Order order = new Order();
    order.setOrderNumber(nextOrderNumber());
    order.setUser(attempt.getUser());
    order.setCustomerName(attempt.getCustomerName());
    order.setCustomerPhone(attempt.getCustomerPhone());
    order.setCustomerEmail(attempt.getCustomerEmail());
    order.setAddress(attempt.getAddress());
    order.setCity(attempt.getCity());
    order.setState(attempt.getState());
    order.setPincode(attempt.getPincode());
    order.setLandmark(attempt.getLandmark());
    order.setPaymentMethod("Razorpay");
    order.setPaymentProvider("RAZORPAY");
    order.setPaymentStatus(PaymentStatus.PAID);
    order.setDeliveryMethod(attempt.getDeliveryMethod());
    order.setEstimatedDelivery(attempt.getEstimatedDelivery());
    order.setStatus(OrderStatus.PLACED);
    order.setSubtotal(attempt.getSubtotal());
    order.setShipping(attempt.getShipping());
    order.setEstimatedTax(attempt.getEstimatedTax());
    order.setDiscountAmount(attempt.getDiscountAmount());
    order.setPromoCode(attempt.getPromoCode());
    order.setTotal(attempt.getTotal());
    order.setRazorpayOrderId(attempt.getRazorpayOrderId());
    order.setRazorpayPaymentId(razorpayPaymentId);
    order.setPaidAt(Instant.now());

    for (RazorpayPaymentAttemptItem attemptItem : attempt.getItems()) {
      Product product = products.findForInventoryUpdate(attemptItem.getProduct().getId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
      if (product.getStock() < attemptItem.getQty()) {
        throw new BusinessRuleException("Insufficient stock for " + attemptItem.getProductName());
      }
      product.setStock(product.getStock() - attemptItem.getQty());

      OrderItem item = new OrderItem();
      item.setOrder(order);
      item.setProduct(product);
      item.setProductSku(attemptItem.getProductSku());
      item.setProductName(attemptItem.getProductName());
      item.setProductImage(attemptItem.getProductImage());
      item.setColor(attemptItem.getColor());
      item.setSize(attemptItem.getSize());
      item.setUnitPrice(attemptItem.getUnitPrice());
      item.setQty(attemptItem.getQty());
      item.setLineTotal(attemptItem.getLineTotal());
      order.getItems().add(item);
    }

    Order saved = orders.save(order);
    history.save(new OrderStatusHistory(saved, null, OrderStatus.PLACED, OrderActorType.SYSTEM,
        "PAYMENT", razorpayPaymentId, "Payment verified and order created"));
    attempt.setRazorpayPaymentId(razorpayPaymentId);
    attempt.setStatus(RazorpayPaymentAttemptStatus.COMPLETED);
    attempt.setCompletedOrder(saved);
    carts.clearForUser(attempt.getUser().getId());
    mail.orderConfirmed(saved);
    return OrderResponse.from(saved);
  }

  private RazorpayPaymentAttempt find(String razorpayOrderId) {
    return attempts.findWithItemsByRazorpayOrderId(razorpayOrderId)
        .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found"));
  }

  private boolean sameCheckout(RazorpayPaymentAttempt attempt, Cart cart, CreateOrderRequest request) {
    if (!Objects.equals(attempt.getCustomerName(), request.fullName())
        || !Objects.equals(attempt.getCustomerPhone(), request.phone())
        || !Objects.equals(attempt.getCustomerEmail(), request.email())
        || !Objects.equals(attempt.getAddress(), request.address())
        || !Objects.equals(attempt.getCity(), request.city())
        || !Objects.equals(attempt.getState(), request.state())
        || !Objects.equals(attempt.getPincode(), request.pincode())
        || !Objects.equals(attempt.getLandmark(), request.landmark())
        || !Objects.equals(attempt.getDeliveryMethod(), blankDefault(request.deliveryMethod(), "Standard Delivery"))
        || !Objects.equals(attempt.getPromoCode(), normalizedPromo(request.promoCode()))
        || attempt.getItems().size() != cart.getItems().size()) {
      return false;
    }
    return cart.getItems().stream().allMatch(cartItem -> attempt.getItems().stream().anyMatch(attemptItem ->
        Objects.equals(attemptItem.getProduct().getId(), cartItem.getProduct().getId())
            && attemptItem.getQty() == cartItem.getQty()
            && Objects.equals(attemptItem.getSize(), cartItem.getSize())
            && Objects.equals(attemptItem.getColor(), cartItem.getColor())
            && attemptItem.getUnitPrice().compareTo(carts.priceFor(cartItem)) == 0));
  }

  private Long currentUserId() {
    return SecuritySupport.currentUser()
        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unauthorized"))
        .id();
  }

  private String nextReceipt() {
    return "PAY" + random.nextInt(100000000, 999999999);
  }

  private String nextOrderNumber() {
    return "IN" + random.nextInt(100000, 999999);
  }

  private String normalizedPromo(String promoCode) {
    return promoCode == null || promoCode.isBlank() ? null : promoCode.trim().toUpperCase();
  }

  private String blankDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private BigDecimal deliveryFee(String deliveryMethod, BigDecimal subtotal) {
    BigDecimal shipping = subtotal.compareTo(MoneyUtils.rupees(1499)) >= 0
        ? BigDecimal.ZERO : MoneyUtils.rupees(149);
    if (deliveryMethod != null && deliveryMethod.startsWith("Express")) {
      shipping = shipping.add(MoneyUtils.rupees(99));
    }
    return shipping;
  }

  public record PendingAttempt(RazorpayPaymentAttempt attempt, boolean reused) {
  }
}
