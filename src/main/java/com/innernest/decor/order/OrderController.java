package com.innernest.decor.order;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;

  OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  OrderResponse create(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                       @Valid @RequestBody CreateOrderRequest request) {
    return service.create(sessionId, request);
  }

  @GetMapping("/my")
  List<OrderResponse> my() {
    return service.my();
  }

  @GetMapping("/{orderNumber}")
  OrderResponse get(@PathVariable String orderNumber,
                    @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
    return service.get(orderNumber, sessionId);
  }
}
