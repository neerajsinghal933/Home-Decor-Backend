ALTER TABLE orders ADD COLUMN payment_provider VARCHAR(30) NULL;
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP NULL;

UPDATE orders
SET payment_provider = 'RAZORPAY'
WHERE razorpay_order_id IS NOT NULL;

CREATE UNIQUE INDEX uk_orders_razorpay_order_id ON orders(razorpay_order_id);
CREATE UNIQUE INDEX uk_orders_razorpay_payment_id ON orders(razorpay_payment_id);

CREATE TABLE razorpay_webhook_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(120) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_razorpay_webhook_event_id UNIQUE (event_id)
);
