package com.innernest.decor.wishlist;

import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.catalog.ProductResponse;
import com.innernest.decor.catalog.ProductStatus;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {
  private final WishlistRepository wishlist;
  private final ProductRepository products;
  private final UserRepository users;

  WishlistService(WishlistRepository wishlist, ProductRepository products, UserRepository users) {
    this.wishlist = wishlist;
    this.products = products;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public WishlistResponse get() {
    Long userId = currentUserId();
    return new WishlistResponse(wishlist.findByUserIdOrderByIdDesc(userId).stream()
        .map(WishlistItem::getProduct)
        .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
        .map(ProductResponse::from)
        .toList());
  }

  @Transactional
  public WishlistResponse add(Long productId) {
    Long userId = currentUserId();
    if (!wishlist.existsByUserIdAndProductId(userId, productId)) {
      var product = products.findById(productId)
          .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
      WishlistItem item = new WishlistItem();
      item.setUser(users.getReferenceById(userId));
      item.setProduct(product);
      wishlist.save(item);
    }
    return get();
  }

  @Transactional
  public WishlistResponse remove(Long productId) {
    Long userId = currentUserId();
    WishlistItem item = wishlist.findByUserIdAndProductId(userId, productId)
        .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
    wishlist.delete(item);
    return get();
  }

  private Long currentUserId() {
    return SecuritySupport.currentUser().orElseThrow(() -> new AccessDeniedException("Unauthorized")).id();
  }
}

