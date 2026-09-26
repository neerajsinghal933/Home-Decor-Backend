package com.innernest.decor.order;

class RefundInProgressException extends RuntimeException {
  RefundInProgressException() { super("Refund request is already being processed"); }
}
