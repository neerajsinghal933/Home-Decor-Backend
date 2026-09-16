package com.innernest.decor.cart;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
  private final CartService service;

  CartController(CartService service) {
    this.service = service;
  }

  @GetMapping
  CartResponse get(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
    return service.get(sessionId);
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  CartResponse add(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                   @Valid @RequestBody AddCartItemRequest request) {
    return service.add(sessionId, request);
  }

  @PatchMapping("/items/{itemId}")
  CartResponse update(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                      @PathVariable Long itemId,
                      @Valid @RequestBody UpdateCartItemRequest request) {
    return service.update(sessionId, itemId, request);
  }

  @DeleteMapping
  CartResponse clear(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
    return service.clear(sessionId);
  }

  @DeleteMapping("/items/{itemId}")
  CartResponse remove(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                      @PathVariable Long itemId) {
    return service.remove(sessionId, itemId);
  }
}
