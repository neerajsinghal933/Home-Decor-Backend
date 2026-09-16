package com.innernest.decor.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final RazorpayPaymentService razorpay;

  PaymentController(RazorpayPaymentService razorpay) {
    this.razorpay = razorpay;
  }

  @PostMapping("/razorpay/orders")
  @ResponseStatus(HttpStatus.CREATED)
  RazorpayCreateOrderResponse createRazorpayOrder(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                                  @Valid @RequestBody CreateOrderRequest request) {
    return razorpay.create(sessionId, request);
  }

  @PostMapping("/razorpay/verify")
  OrderResponse verifyRazorpayPayment(@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
                                      @Valid @RequestBody RazorpayVerifyRequest request) {
    return razorpay.verify(sessionId, request);
  }
}
