package com.innernest.decor.wishlist;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
  private final WishlistService service;

  WishlistController(WishlistService service) {
    this.service = service;
  }

  @GetMapping
  WishlistResponse get() {
    return service.get();
  }

  @PostMapping("/items/{productId}")
  WishlistResponse add(@PathVariable Long productId) {
    return service.add(productId);
  }

  @DeleteMapping("/items/{productId}")
  WishlistResponse remove(@PathVariable Long productId) {
    return service.remove(productId);
  }
}
