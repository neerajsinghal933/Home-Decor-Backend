package com.innernest.decor.order;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TransactionalOrderMailService {
  private final TransactionalEmailEventRepository events;
  private final ApplicationEventPublisher publisher;
  private final String adminEmail;
  private final String frontendUrl;

  TransactionalOrderMailService(TransactionalEmailEventRepository events, ApplicationEventPublisher publisher,
      @Value("${app.mail.contact-to:innernestofficial@gmail.com}") String adminEmail,
      @Value("${app.frontend-url:https://www.innernestdecor.com}") String frontendUrl) {
    this.events = events; this.publisher = publisher; this.adminEmail = adminEmail; this.frontendUrl = frontendUrl;
  }

  public void orderConfirmed(Order order) {
    String items = order.getItems().stream().map(i -> i.getProductName() + " × " + i.getQty()).reduce((a, b) -> a + "\n" + b).orElse("");
    queue("ORDER_CONFIRMED:" + order.getOrderNumber(), "ORDER_CONFIRMED", order.getCustomerEmail(),
        "Order confirmed - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "Your payment was successful and your order is confirmed.\n\nOrder: #" + order.getOrderNumber()
            + "\nOrder date: " + order.getCreatedAt() + "\nItems:\n" + items + "\nAmount: " + money(order.getTotal()) + "\nPayment: Paid\n\nView your order: " + frontendUrl + "/#orders" + signoff());
  }

  public void paymentFailed(RazorpayPaymentAttempt attempt) {
    queue("PAYMENT_FAILED:" + attempt.getReceipt(), "PAYMENT_FAILED", attempt.getCustomerEmail(),
        "Payment unsuccessful - Inner Nest", "Hi " + attempt.getCustomerName() + ",\n\nWe could not confirm your payment for "
            + money(attempt.getTotal()) + ". No order was placed and your cart remains available.\nCheckout reference: "
            + attempt.getReceipt() + "\nYou can safely return to checkout and retry.\n\n"
            + frontendUrl + "/#checkout" + signoff());
  }

  public void requestSubmitted(OrderServiceRequest request) {
    String label = request.getType() == OrderRequestType.CANCELLATION ? "cancellation" : "return / refund";
    Order order = request.getOrder();
    queue(request.getType() + "_REQUESTED:" + request.getId(), request.getType() + "_REQUESTED", order.getCustomerEmail(),
        capitalize(label) + " request received - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "We received your " + label + " request for order #" + order.getOrderNumber()
            + ". Our team will review it and update you.\n\nReason: " + request.getReason() + signoff());
    queue("ADMIN_" + request.getType() + "_REQUESTED:" + request.getId(), "ADMIN_REQUEST", adminEmail,
        "New " + label + " request - #" + order.getOrderNumber(),
        "A customer submitted a " + label + " request.\n\nOrder: #" + order.getOrderNumber() + "\nCustomer: "
            + order.getCustomerName() + " (" + order.getCustomerEmail() + ")\nAmount: " + money(order.getTotal())
            + "\nReason: " + request.getReason() + "\n\nReview it in the Inner Nest admin dashboard.");
  }

  public void requestRejected(OrderServiceRequest request) {
    Order order = request.getOrder();
    String label = request.getType() == OrderRequestType.CANCELLATION ? "Cancellation" : "Return";
    queue(request.getType() + "_REJECTED:" + request.getId(), request.getType() + "_REJECTED", order.getCustomerEmail(),
        label + " request update - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "Your " + label.toLowerCase(Locale.ROOT) + " request was not approved.\n\n"
            + (request.getAdminNote() == null ? "Please contact us if you need help." : "Reason: " + request.getAdminNote()) + signoff());
  }

  public void requestApproved(OrderServiceRequest request) {
    Order order = request.getOrder();
    String label = request.getType() == OrderRequestType.CANCELLATION ? "Cancellation" : "Return";
    queue(request.getType() + "_APPROVED:" + request.getId(), request.getType() + "_APPROVED", order.getCustomerEmail(),
        label + " approved - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "Your " + label.toLowerCase(Locale.ROOT) + " request was approved. A refund is being initiated separately and may take time to process." + signoff());
  }

  public void refundInitiated(RefundTransaction refund) {
    Order order = refund.getOrder();
    queue("REFUND_PENDING:" + refund.getId(), "REFUND_PENDING", order.getCustomerEmail(),
        "Refund initiated - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "We initiated a refund of " + money(refund.getAmount()) + " to your original payment method."
            + "\nRefund reference: " + refund.getRazorpayRefundId() + "\nNormal processing can take several working days." + signoff());
  }

  public void refundCompleted(RefundTransaction refund) {
    Order order = refund.getOrder();
    queue("REFUND_PROCESSED:" + refund.getId(), "REFUND_PROCESSED", order.getCustomerEmail(),
        "Refund processed - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "Razorpay has processed your refund of " + money(refund.getAmount()) + "."
            + "\nRefund reference: " + (refund.getRefundReference() == null ? refund.getRazorpayRefundId() : refund.getRefundReference())
            + "\nYour bank may take additional time to display the credit." + signoff());
  }

  public void refundFailed(RefundTransaction refund) {
    Order order = refund.getOrder();
    queue("REFUND_FAILED:" + refund.getId(), "REFUND_FAILED", order.getCustomerEmail(),
        "Refund update - Inner Nest #" + order.getOrderNumber(),
        hello(order) + "Your refund needs attention. Our team has been notified and will handle it. You do not need to submit another request." + signoff());
    queue("ADMIN_REFUND_FAILED:" + refund.getId(), "ADMIN_REFUND_FAILED", adminEmail,
        "Refund requires attention - #" + order.getOrderNumber(),
        "Refund " + refund.getId() + " for order #" + order.getOrderNumber() + " requires attention. Review it in the admin dashboard. No sensitive gateway details are included in this email.");
  }

  private void queue(String key, String type, String recipient, String subject, String body) {
    if (recipient == null || recipient.isBlank() || events.existsByEventKey(key)) return;
    TransactionalEmailEvent saved = events.save(new TransactionalEmailEvent(key, type, recipient, subject, body));
    publisher.publishEvent(new EmailQueued(saved.getId()));
  }

  private String hello(Order order) { return "Hi " + order.getCustomerName() + ",\n\n"; }
  private String signoff() { return "\n\nWarmly,\nInner Nest Decor"; }
  private String capitalize(String value) { return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }
  private String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value); }
  record EmailQueued(Long id) {}
}
