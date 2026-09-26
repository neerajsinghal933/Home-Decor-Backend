package com.innernest.decor.order;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionalEmailListener {
  private final TransactionalEmailDelivery delivery;
  TransactionalEmailListener(TransactionalEmailDelivery delivery) { this.delivery = delivery; }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onQueued(TransactionalOrderMailService.EmailQueued event) {
    delivery.deliver(event.id());
  }
}
