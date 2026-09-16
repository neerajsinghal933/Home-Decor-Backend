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
import com.innernest.decor.user.UserRole;
import com.innernest.decor.user.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderService {
  private final OrderRepository orders;
  private final ProductRepository products;
  private final CartService carts;
  private final UserRepository users;
  private final PromoCodeService promos;
  private final SecureRandom random = new SecureRandom();

  OrderService(OrderRepository orders, ProductRepository products, CartService carts, UserRepository users, PromoCodeService promos) {
    this.orders = orders;
    this.products = products;
    this.carts = carts;
    this.users = users;
    this.promos = promos;
  }

  @Transactional
  public OrderResponse create(String sessionId, CreateOrderRequest request) {
    String paymentMethod = blankDefault(request.paymentMethod(), "");
    if (!paymentMethod.equalsIgnoreCase("Cash on Delivery")) {
      throw new BusinessRuleException("Online payments must be completed through Razorpay");
    }
    Order saved = createOrder(sessionId, request, PaymentStatus.PENDING, true);
    return OrderResponse.from(saved);
  }

  @Transactional
  public Order createPendingPaymentOrder(String sessionId, CreateOrderRequest request) {
    return createOrder(sessionId, request, PaymentStatus.PENDING, false);
  }

  @Transactional
  public OrderResponse markPaid(String sessionId, String orderNumber, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
    Order order = orders.findWithItemsByOrderNumber(orderNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (order.getPaymentStatus() == PaymentStatus.PAID) return OrderResponse.from(order);
    if (order.getPaymentStatus() != PaymentStatus.PENDING) throw new BusinessRuleException("Order is not awaiting payment");
    if (order.getRazorpayOrderId() == null || !order.getRazorpayOrderId().equals(razorpayOrderId)) {
      throw new BusinessRuleException("Payment order mismatch");
    }
    reserveInventory(order);
    order.setPaymentStatus(PaymentStatus.PAID);
    order.setRazorpayPaymentId(razorpayPaymentId);
    order.setRazorpaySignature(razorpaySignature);
    Cart cart = carts.loadOrCreate(sessionId);
    cart.getItems().clear();
    return OrderResponse.from(orders.save(order));
  }

  @Transactional
  public void attachRazorpayOrderId(String orderNumber, String razorpayOrderId) {
    Order order = orders.findWithItemsByOrderNumber(orderNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    order.setRazorpayOrderId(razorpayOrderId);
  }

  private Order createOrder(String sessionId, CreateOrderRequest request, PaymentStatus paymentStatus, boolean reserveInventory) {
    Cart cart = carts.loadForCheckout(sessionId);
    if (cart.getItems().isEmpty()) {
      throw new DuplicateResourceException("Cart is empty or has already been checked out");
    }

    Order order = new Order();
    order.setOrderNumber(nextOrderNumber());
    SecuritySupport.currentUser().ifPresent(principal -> order.setUser(users.getReferenceById(principal.id())));
    if (order.getUser() == null) {
      if (!StringUtils.hasText(sessionId)) {
        throw new BusinessRuleException("Session ID is required for guest checkout");
      }
      order.setGuestSessionId(carts.normalizedSessionId(sessionId));
    }
    order.setCustomerName(request.fullName());
    order.setCustomerPhone(request.phone());
    order.setCustomerEmail(request.email());
    order.setAddress(request.address());
    order.setCity(request.city());
    order.setState(request.state());
    order.setPincode(request.pincode());
    order.setLandmark(request.landmark());
    order.setPaymentMethod(blankDefault(request.paymentMethod(), "UPI"));
    order.setPaymentStatus(paymentStatus);
    order.setDeliveryMethod(blankDefault(request.deliveryMethod(), "Standard Delivery"));
    order.setEstimatedDelivery(order.getDeliveryMethod().startsWith("Express") ? "1-2 working days" : "3-5 working days");

    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem cartItem : cart.getItems()) {
      Product product = reserveInventory
          ? products.findForInventoryUpdate(cartItem.getProduct().getId()).orElseThrow(() -> new ResourceNotFoundException("Product not found"))
          : cartItem.getProduct();
      if (product.getStock() < cartItem.getQty()) {
        throw new BusinessRuleException("Insufficient stock for " + product.getName());
      }
      if (reserveInventory) product.setStock(product.getStock() - cartItem.getQty());
      BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQty()));
      subtotal = subtotal.add(lineTotal);

      OrderItem item = new OrderItem();
      item.setOrder(order);
      item.setProduct(product);
      item.setProductSku(product.getSku());
      item.setProductName(product.getName());
      item.setProductImage(product.getPrimaryImage());
      item.setColor(cartItem.getColor());
      item.setSize(cartItem.getSize());
      item.setUnitPrice(product.getPrice());
      item.setQty(cartItem.getQty());
      item.setLineTotal(lineTotal);
      order.getItems().add(item);
    }
    BigDecimal shipping = deliveryFee(order.getDeliveryMethod(), order.getPaymentMethod(), subtotal);
    BigDecimal tax = MoneyUtils.tax(subtotal);
    BigDecimal discount = promos.discountFor(request.promoCode(), subtotal);
    order.setSubtotal(subtotal);
    order.setShipping(shipping);
    order.setEstimatedTax(tax);
    order.setDiscountAmount(discount);
    order.setPromoCode(request.promoCode() == null || request.promoCode().isBlank() ? null : request.promoCode().trim().toUpperCase());
    order.setTotal(subtotal.add(shipping).add(tax).subtract(discount));
    Order saved = orders.save(order);
    if (reserveInventory) cart.getItems().clear();
    return saved;
  }

  @Transactional(readOnly = true)
  public OrderResponse get(String orderNumber, String sessionId) {
    Order order = orders.findWithItemsByOrderNumber(orderNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    var principal = SecuritySupport.currentUser();
    boolean admin = principal.map(user -> user.role() == UserRole.ADMIN).orElse(false);
    boolean userOwnsOrder = order.getUser() != null
        && principal.map(user -> order.getUser().getId().equals(user.id())).orElse(false);
    boolean guestOwnsOrder = order.getUser() == null
        && StringUtils.hasText(sessionId)
        && order.getGuestSessionId() != null
        && order.getGuestSessionId().equals(carts.normalizedSessionId(sessionId));
    if (!admin && !userOwnsOrder && !guestOwnsOrder) {
      // Avoid confirming whether an order number exists to unauthorized callers.
      throw new ResourceNotFoundException("Order not found");
    }
    return OrderResponse.from(order);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> my() {
    Long userId = SecuritySupport.currentUser()
        .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unauthorized"))
        .id();
    return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
  }

  private String nextOrderNumber() {
    return "IN" + random.nextInt(100000, 999999);
  }

  private String blankDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private void reserveInventory(Order order) {
    for (OrderItem item : order.getItems()) {
      Product product = products.findForInventoryUpdate(item.getProduct().getId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
      if (product.getStock() < item.getQty()) throw new BusinessRuleException("Insufficient stock for " + product.getName());
      product.setStock(product.getStock() - item.getQty());
    }
  }

  private BigDecimal deliveryFee(String deliveryMethod, String paymentMethod, BigDecimal subtotal) {
    BigDecimal shipping = subtotal.compareTo(MoneyUtils.rupees(1499)) >= 0 ? BigDecimal.ZERO : MoneyUtils.rupees(149);
    if (deliveryMethod != null && deliveryMethod.startsWith("Express")) shipping = shipping.add(MoneyUtils.rupees(99));
    if (paymentMethod != null && paymentMethod.toLowerCase().contains("cash on delivery")) shipping = shipping.add(MoneyUtils.rupees(49));
    return shipping;
  }
}
