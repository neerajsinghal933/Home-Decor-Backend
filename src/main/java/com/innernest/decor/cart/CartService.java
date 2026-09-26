package com.innernest.decor.cart;

import com.innernest.decor.catalog.Product;
import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.DuplicateResourceException;
import com.innernest.decor.common.MoneyUtils;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.UserRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CartService {
  private static final Pattern SESSION_ID = Pattern.compile("[A-Za-z0-9._:-]{16,120}");

  private final CartRepository carts;
  private final ProductRepository products;
  private final UserRepository users;

  CartService(CartRepository carts, ProductRepository products, UserRepository users) {
    this.carts = carts;
    this.products = products;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public CartResponse get(String sessionId) {
    return toResponse(loadExisting(sessionId).orElseGet(Cart::new));
  }

  @Transactional
  public CartResponse add(String sessionId, AddCartItemRequest request) {
    Product product = products.findById(request.productId())
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    int qty = request.qty() <= 0 ? 1 : request.qty();
    if (product.getStock() < qty) throw new BusinessRuleException("Insufficient stock for " + product.getName());

    Cart cart = loadOrCreate(sessionId);
    String size = selectedSize(product, request.size());
    String color = StringUtils.hasText(request.color()) ? request.color() : product.getColor();
    CartItem item = cart.getItems().stream()
        .filter(entry -> Objects.equals(entry.getProduct().getId(), product.getId())
            && Objects.equals(entry.getSize(), size)
            && Objects.equals(entry.getColor(), color))
        .findFirst()
        .orElseGet(() -> {
          CartItem created = new CartItem();
          created.setCart(cart);
          created.setProduct(product);
          created.setSize(size);
          created.setColor(color);
          cart.getItems().add(created);
          return created;
        });
    int nextQty = item.getQty() + qty;
    if (product.getStock() < nextQty) throw new BusinessRuleException("Insufficient stock for " + product.getName());
    item.setQty(nextQty);
    return toResponse(carts.save(cart));
  }

  @Transactional
  public CartResponse update(String sessionId, Long itemId, UpdateCartItemRequest request) {
    Cart cart = loadOrCreate(sessionId);
    CartItem item = cart.getItems().stream()
        .filter(entry -> Objects.equals(entry.getId(), itemId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    if (item.getProduct().getStock() < request.qty()) throw new BusinessRuleException("Insufficient stock for " + item.getProduct().getName());
    item.setQty(request.qty());
    return toResponse(carts.save(cart));
  }

  @Transactional
  public CartResponse clear(String sessionId) {
    Cart cart = loadOrCreate(sessionId);
    cart.getItems().clear();
    return toResponse(carts.save(cart));
  }

  @Transactional
  public CartResponse remove(String sessionId, Long itemId) {
    Cart cart = loadOrCreate(sessionId);
    boolean removed = cart.getItems().removeIf(entry -> Objects.equals(entry.getId(), itemId));
    if (!removed) throw new ResourceNotFoundException("Cart item not found");
    return toResponse(carts.save(cart));
  }

  @Transactional
  public Cart loadOrCreate(String sessionId) {
    return loadExisting(sessionId).orElseGet(() -> {
      Cart cart = new Cart();
      SecuritySupport.currentUser().ifPresent(principal -> cart.setUser(users.getReferenceById(principal.id())));
      if (cart.getUser() == null) cart.setSessionId(normalizedSessionId(sessionId));
      return carts.save(cart);
    });
  }

  @Transactional
  public Cart loadForCheckout(String sessionId) {
    var principal = SecuritySupport.currentUser();
    return (principal.isPresent()
        ? carts.findByUserIdForCheckout(principal.get().id())
        : carts.findBySessionIdForCheckout(normalizedSessionId(sessionId)))
        .orElseThrow(() -> new DuplicateResourceException("Cart is empty or has already been checked out"));
  }

  @Transactional
  public void clearForUser(Long userId) {
    carts.findByUserIdForCheckout(userId).ifPresent(cart -> cart.getItems().clear());
  }

  private java.util.Optional<Cart> loadExisting(String sessionId) {
    var principal = SecuritySupport.currentUser();
    if (principal.isPresent()) {
      return carts.findWithItemsByUserId(principal.get().id());
    }
    return carts.findWithItemsBySessionId(normalizedSessionId(sessionId));
  }

  public String normalizedSessionId(String sessionId) {
    if (!StringUtils.hasText(sessionId)) {
      throw new BusinessRuleException("Session ID is required for guest cart access");
    }
    String normalized = sessionId.trim();
    if (!SESSION_ID.matcher(normalized).matches()) {
      throw new BusinessRuleException("Session ID must be 16-120 letters, numbers, dots, underscores, colons, or hyphens");
    }
    return normalized;
  }

  public CartResponse toResponse(Cart cart) {
    var items = cart.getItems().stream().map(CartItemResponse::from).toList();
    BigDecimal subtotal = cart.getItems().stream()
        .map(item -> priceFor(item).multiply(BigDecimal.valueOf(item.getQty())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal shipping = subtotal.compareTo(MoneyUtils.rupees(1499)) >= 0 || subtotal.signum() == 0 ? BigDecimal.ZERO : MoneyUtils.rupees(149);
    BigDecimal tax = MoneyUtils.tax(subtotal);
    int itemCount = cart.getItems().stream().mapToInt(CartItem::getQty).sum();
    return new CartResponse(items, new CartTotalsResponse(itemCount, subtotal, shipping, tax, subtotal.add(shipping).add(tax)));
  }

  public BigDecimal priceFor(CartItem item) {
    Product product = item.getProduct();
    if (product.hasSizeVariants() && product.findSizeVariant(item.getSize()).isEmpty()) {
      throw new BusinessRuleException("The selected size is no longer available for " + product.getName());
    }
    return product.priceForSize(item.getSize());
  }

  private String selectedSize(Product product, String requestedSize) {
    if (!product.hasSizeVariants()) return StringUtils.hasText(requestedSize) ? requestedSize.trim() : "Medium";
    if (!StringUtils.hasText(requestedSize)) return product.defaultSize();
    return product.findSizeVariant(requestedSize)
        .map(variant -> variant.getSizeLabel())
        .orElseThrow(() -> new BusinessRuleException("Please select an available size for " + product.getName()));
  }
}
