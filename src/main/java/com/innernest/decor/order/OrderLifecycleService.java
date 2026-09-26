package com.innernest.decor.order;

import com.innernest.decor.catalog.Product;
import com.innernest.decor.catalog.ProductRepository;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderLifecycleService {
  private final OrderRepository orders;
  private final OrderServiceRequestRepository requests;
  private final RefundTransactionRepository refunds;
  private final OrderStatusHistoryRepository history;
  private final ProductRepository products;
  private final UserRepository users;
  private final RazorpayRefundsClient razorpay;
  private final TransactionalOrderMailService mail;
  private final TransactionTemplate transaction;

  OrderLifecycleService(OrderRepository orders, OrderServiceRequestRepository requests,
      RefundTransactionRepository refunds, OrderStatusHistoryRepository history, ProductRepository products,
      UserRepository users, RazorpayRefundsClient razorpay, TransactionalOrderMailService mail,
      PlatformTransactionManager transactionManager) {
    this.orders = orders; this.requests = requests; this.refunds = refunds; this.history = history;
    this.products = products; this.users = users; this.razorpay = razorpay; this.mail = mail;
    this.transaction = new TransactionTemplate(transactionManager);
  }

  @Transactional(readOnly = true)
  public OrderLifecycleResponse customerDetails(String orderNumber) {
    Order order = ownedOrder(orderNumber, false);
    return response(order);
  }

  @Transactional
  public OrderLifecycleResponse requestCancellation(String orderNumber, OrderActionRequest input) {
    Order order = ownedOrder(orderNumber, true);
    if (!canCancel(order)) throw new BusinessRuleException("This order can no longer be cancelled");
    rejectDuplicateRequest(order);
    OrderServiceRequest request = createRequest(order, OrderRequestType.CANCELLATION, input);
    transition(order, OrderStatus.CANCELLATION_REQUESTED, OrderActorType.CUSTOMER, "REQUEST", String.valueOf(request.getId()), request.getReason());
    mail.requestSubmitted(request);
    return response(order);
  }

  @Transactional
  public OrderLifecycleResponse requestReturn(String orderNumber, OrderActionRequest input) {
    Order order = ownedOrder(orderNumber, true);
    if (!canReturn(order)) throw new BusinessRuleException("This order is not eligible for return");
    rejectDuplicateRequest(order);
    OrderServiceRequest request = createRequest(order, OrderRequestType.RETURN, input);
    transition(order, OrderStatus.RETURN_REQUESTED, OrderActorType.CUSTOMER, "REQUEST", String.valueOf(request.getId()), request.getReason());
    mail.requestSubmitted(request);
    return response(order);
  }

  @Transactional(readOnly = true)
  public List<AdminOrderRequestResponse> adminRequests(String type, String status, String refundStatus) {
    return requests.findAllByOrderByRequestedAtDesc().stream()
        .filter(r -> type == null || type.isBlank() || r.getType().name().equalsIgnoreCase(type))
        .filter(r -> status == null || status.isBlank() || r.getStatus().name().equalsIgnoreCase(status))
        .filter(r -> refundStatus == null || refundStatus.isBlank() || refunds.findByServiceRequestId(r.getId())
            .map(refund -> refund.getStatus().name().equalsIgnoreCase(refundStatus)).orElse(false))
        .map(this::adminResponse).toList();
  }

  public AdminOrderRequestResponse approve(Long requestId, AdminRequestDecision decision) {
    PreparedRefund prepared = transaction.execute(status -> prepareRefund(requestId, decision));
    if (prepared == null || !prepared.submit()) return transaction.execute(status -> adminResponse(
        requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"))));
    try {
      RazorpayRefundResult result = razorpay.createNormalRefund(prepared.paymentId(),
          RazorpayPaymentService.toPaise(prepared.amount()), prepared.receipt(), prepared.orderNumber(), prepared.idempotencyKey());
      transaction.executeWithoutResult(status -> markRefundInitiated(prepared.refundId(), result));
    } catch (RefundInProgressException ex) {
      // Another identical request is active at Razorpay. Keep CREATING so a retry uses the same durable key.
    } catch (BusinessRuleException ex) {
      transaction.executeWithoutResult(status -> markRefundFailed(prepared.refundId()));
    }
    return transaction.execute(status -> adminResponse(
        requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"))));
  }

  private PreparedRefund prepareRefund(Long requestId, AdminRequestDecision decision) {
    OrderServiceRequest initial = requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    Order order = orders.findWithItemsForUpdateByOrderNumber(initial.getOrder().getOrderNumber())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    OrderServiceRequest request = requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    RefundTransaction existing = refunds.findByServiceRequestId(requestId).orElse(null);
    if (request.getStatus() == OrderRequestStatus.APPROVED && existing != null) {
      boolean submit = existing.getStatus() == RefundStatus.CREATING || existing.getStatus() == RefundStatus.FAILED;
      if (submit) { existing.setStatus(RefundStatus.CREATING); existing.setFailureReason(null); }
      return prepared(existing, submit);
    }
    if (request.getStatus() != OrderRequestStatus.REQUESTED) throw new BusinessRuleException("This request has already been reviewed");
    validateApprovalState(order, request);
    request.setStatus(OrderRequestStatus.APPROVED);
    request.setAdminNote(clean(decision.note()));
    request.setReviewedAt(Instant.now());
    request.setReviewedBy(users.getReferenceById(currentAdminId()));
    OrderStatus approvedStatus = request.getType() == OrderRequestType.CANCELLATION ? OrderStatus.CANCELLED : OrderStatus.RETURN_APPROVED;
    transition(order, approvedStatus, OrderActorType.ADMIN, "REQUEST", String.valueOf(request.getId()), request.getAdminNote());
    if (request.getType() == OrderRequestType.CANCELLATION) restoreInventory(order);
    mail.requestApproved(request);
    RefundTransaction refund = new RefundTransaction();
    refund.setOrder(order); refund.setServiceRequest(request); refund.setRazorpayPaymentId(order.getRazorpayPaymentId());
    refund.setAmount(refundableAmount(order));
    refund.setIdempotencyKey("innernest-refund-" + UUID.randomUUID());
    refund.setReceipt("REF-" + order.getOrderNumber() + "-" + request.getId());
    refunds.saveAndFlush(refund);
    return prepared(refund, true);
  }

  private PreparedRefund prepared(RefundTransaction refund, boolean submit) {
    return new PreparedRefund(refund.getId(), refund.getOrder().getOrderNumber(), refund.getRazorpayPaymentId(),
        refund.getAmount(), refund.getReceipt(), refund.getIdempotencyKey(), submit);
  }

  private void markRefundInitiated(Long refundId, RazorpayRefundResult result) {
    RefundTransaction refund = refunds.findById(refundId).orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
    if (refund.getStatus() == RefundStatus.PROCESSED) return;
    refund.setRazorpayRefundId(result.id()); refund.setRefundReference(result.reference());
    refund.setStatus(RefundStatus.PENDING); refund.setInitiatedAt(Instant.now());
    refund.getOrder().setPaymentStatus(PaymentStatus.REFUND_PENDING);
    mail.refundInitiated(refund);
  }

  private void markRefundFailed(Long refundId) {
    RefundTransaction refund = refunds.findById(refundId).orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
    if (refund.getStatus() == RefundStatus.PROCESSED) return;
    refund.setStatus(RefundStatus.FAILED);
    refund.setFailureReason("Refund initiation failed; administrator review required");
    refund.getOrder().setPaymentStatus(PaymentStatus.REFUND_FAILED);
    mail.refundFailed(refund);
  }

  @Transactional
  public AdminOrderRequestResponse reject(Long requestId, AdminRequestDecision decision) {
    if (decision.note() == null || decision.note().isBlank()) throw new BusinessRuleException("A customer-safe rejection reason is required");
    OrderServiceRequest initial = requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    Order order = orders.findWithItemsForUpdateByOrderNumber(initial.getOrder().getOrderNumber())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    OrderServiceRequest request = requests.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    if (request.getStatus() != OrderRequestStatus.REQUESTED) throw new BusinessRuleException("This request has already been reviewed");
    request.setStatus(OrderRequestStatus.REJECTED);
    request.setAdminNote(clean(decision.note()));
    request.setReviewedAt(Instant.now());
    request.setReviewedBy(users.getReferenceById(currentAdminId()));
    OrderStatus next = request.getType() == OrderRequestType.CANCELLATION ? request.getPreviousOrderStatus() : OrderStatus.RETURN_REJECTED;
    transition(order, next, OrderActorType.ADMIN, "REQUEST", String.valueOf(request.getId()), request.getAdminNote());
    mail.requestRejected(request);
    return adminResponse(request);
  }

  @Transactional
  public void applyRefundWebhook(String eventType, String refundId, String receipt, String paymentId, long amountPaise,
                                 String currency, String reference) {
    RefundTransaction refund = refunds.findByRazorpayRefundId(refundId)
        .or(() -> receipt == null || receipt.isBlank() ? java.util.Optional.empty() : refunds.findByReceipt(receipt)).orElse(null);
    if (refund == null) return;
    Order order = orders.findWithItemsForUpdateByOrderNumber(refund.getOrder().getOrderNumber())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (!refund.getRazorpayPaymentId().equals(paymentId) || !order.getRazorpayPaymentId().equals(paymentId)
        || RazorpayPaymentService.toPaise(refund.getAmount()) != amountPaise || !"INR".equals(currency)) {
      throw new BusinessRuleException("Refund webhook does not match the stored refund");
    }
    if (refund.getStatus() == RefundStatus.PROCESSED) return;
    if (refund.getRazorpayRefundId() == null) refund.setRazorpayRefundId(refundId);
    if (reference != null && !reference.isBlank()) refund.setRefundReference(reference);
    if ("refund.processed".equals(eventType)) {
      refund.setStatus(RefundStatus.PROCESSED);
      refund.setCompletedAt(Instant.now());
      updateAggregatePaymentStatus(order);
      mail.refundCompleted(refund);
    } else if ("refund.failed".equals(eventType)) {
      refund.setStatus(RefundStatus.FAILED);
      refund.setFailureReason("Razorpay reported that the refund could not be processed");
      updateAggregatePaymentStatus(order);
      mail.refundFailed(refund);
    } else if ("refund.created".equals(eventType) && refund.getStatus() == RefundStatus.CREATING) {
      refund.setStatus(RefundStatus.PENDING);
      refund.setInitiatedAt(Instant.now());
      updateAggregatePaymentStatus(order);
      mail.refundInitiated(refund);
    }
  }

  boolean canCancel(Order order) {
    return order.getPaymentStatus() == PaymentStatus.PAID
        && (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PROCESSING);
  }

  boolean canReturn(Order order) {
    return order.getPaymentStatus() == PaymentStatus.PAID && order.getStatus() == OrderStatus.DELIVERED;
  }

  private OrderServiceRequest createRequest(Order order, OrderRequestType type, OrderActionRequest input) {
    OrderServiceRequest request = new OrderServiceRequest();
    request.setOrder(order); request.setType(type); request.setPreviousOrderStatus(order.getStatus());
    request.setReason(input.reason().trim()); request.setDetails(clean(input.details()));
    return requests.save(request);
  }

  private void rejectDuplicateRequest(Order order) {
    if (requests.findFirstByOrderIdAndStatusOrderByRequestedAtDesc(order.getId(), OrderRequestStatus.REQUESTED).isPresent()) {
      throw new BusinessRuleException("A request for this order is already awaiting review");
    }
  }

  private void validateApprovalState(Order order, OrderServiceRequest request) {
    if (order.getPaymentStatus() != PaymentStatus.PAID || order.getRazorpayPaymentId() == null) {
      throw new BusinessRuleException("Only a verified Razorpay payment can be refunded");
    }
    if (request.getType() == OrderRequestType.CANCELLATION && order.getStatus() != OrderStatus.CANCELLATION_REQUESTED) {
      throw new BusinessRuleException("Cancellation request is no longer eligible");
    }
    if (request.getType() == OrderRequestType.RETURN && order.getStatus() != OrderStatus.RETURN_REQUESTED) {
      throw new BusinessRuleException("Return request is no longer eligible");
    }
  }

  private BigDecimal refundableAmount(Order order) {
    BigDecimal reserved = refunds.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
        .filter(r -> r.getStatus() == RefundStatus.CREATING || r.getStatus() == RefundStatus.PENDING || r.getStatus() == RefundStatus.PROCESSED)
        .map(RefundTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal available = order.getTotal().subtract(reserved);
    if (available.signum() <= 0) throw new BusinessRuleException("This payment has already been fully refunded");
    return available;
  }

  private void updateAggregatePaymentStatus(Order order) {
    List<RefundTransaction> rows = refunds.findByOrderIdOrderByCreatedAtDesc(order.getId());
    BigDecimal processed = rows.stream().filter(r -> r.getStatus() == RefundStatus.PROCESSED).map(RefundTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean pending = rows.stream().anyMatch(r -> r.getStatus() == RefundStatus.PENDING || r.getStatus() == RefundStatus.CREATING);
    boolean failed = rows.stream().anyMatch(r -> r.getStatus() == RefundStatus.FAILED);
    if (processed.compareTo(order.getTotal()) >= 0) order.setPaymentStatus(PaymentStatus.REFUNDED);
    else if (pending) order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
    else if (processed.signum() > 0) order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
    else if (failed) order.setPaymentStatus(PaymentStatus.REFUND_FAILED);
    else order.setPaymentStatus(PaymentStatus.PAID);
  }

  private void restoreInventory(Order order) {
    for (OrderItem item : order.getItems()) {
      if (item.getProduct() == null) continue;
      Product product = products.findForInventoryUpdate(item.getProduct().getId()).orElse(null);
      if (product != null) product.setStock(product.getStock() + item.getQty());
    }
  }

  private void transition(Order order, OrderStatus next, OrderActorType actor, String refType, String refId, String note) {
    OrderStatus previous = order.getStatus();
    if (previous == next) return;
    order.setStatus(next);
    history.save(new OrderStatusHistory(order, previous, next, actor, refType, refId, clean(note)));
  }

  private Order ownedOrder(String orderNumber, boolean lock) {
    Long userId = SecuritySupport.currentUser().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unauthorized")).id();
    Order order = (lock ? orders.findWithItemsForUpdateByOrderNumber(orderNumber) : orders.findWithItemsByOrderNumber(orderNumber))
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (order.getUser() == null || !order.getUser().getId().equals(userId)) throw new ResourceNotFoundException("Order not found");
    return order;
  }

  private Long currentAdminId() {
    return SecuritySupport.currentUser().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unauthorized")).id();
  }

  private OrderLifecycleResponse response(Order order) {
    OrderServiceRequest latest = requests.findFirstByOrderIdOrderByRequestedAtDesc(order.getId()).orElse(null);
    return new OrderLifecycleResponse(OrderResponse.from(order), canCancel(order), canReturn(order), OrderRequestResponse.from(latest),
        refunds.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream().map(RefundResponse::from).toList(),
        history.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream().map(OrderHistoryResponse::from).toList());
  }

  private AdminOrderRequestResponse adminResponse(OrderServiceRequest request) {
    Order order = request.getOrder();
    RefundTransaction refund = refunds.findByServiceRequestId(request.getId()).orElse(null);
    return new AdminOrderRequestResponse(request.getId(), request.getType().name(), request.getStatus().name(), order.getOrderNumber(),
        order.getCustomerName(), order.getCustomerEmail(), order.getCreatedAt(), order.getTotal(), order.getPaymentStatus().name(),
        order.getStatus().name(), order.getRazorpayPaymentId(), request.getReason(), request.getDetails(), request.getAdminNote(),
        request.getRequestedAt(), RefundResponse.from(refund));
  }

  private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

  private record PreparedRefund(Long refundId, String orderNumber, String paymentId, BigDecimal amount,
                                String receipt, String idempotencyKey, boolean submit) {}
}
