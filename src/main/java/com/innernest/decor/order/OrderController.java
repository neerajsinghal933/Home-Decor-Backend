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
  private final OrderLifecycleService lifecycle;

  OrderController(OrderService service, OrderLifecycleService lifecycle) {
    this.service = service;
    this.lifecycle = lifecycle;
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

  @GetMapping("/my/{orderNumber}/lifecycle")
  OrderLifecycleResponse lifecycle(@PathVariable String orderNumber) {
    return lifecycle.customerDetails(orderNumber);
  }

  @PostMapping("/my/{orderNumber}/cancellation")
  OrderLifecycleResponse cancel(@PathVariable String orderNumber, @Valid @RequestBody OrderActionRequest request) {
    return lifecycle.requestCancellation(orderNumber, request);
  }

  @PostMapping("/my/{orderNumber}/return")
  OrderLifecycleResponse requestReturn(@PathVariable String orderNumber, @Valid @RequestBody OrderActionRequest request) {
    return lifecycle.requestReturn(orderNumber, request);
  }
}
