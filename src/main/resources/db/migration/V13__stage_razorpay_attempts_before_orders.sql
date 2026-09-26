CREATE TABLE razorpay_payment_attempts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  receipt VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  razorpay_order_id VARCHAR(120),
  razorpay_payment_id VARCHAR(120),
  status VARCHAR(30) NOT NULL,
  customer_name VARCHAR(160) NOT NULL,
  customer_phone VARCHAR(40) NOT NULL,
  customer_email VARCHAR(180) NOT NULL,
  address VARCHAR(300) NOT NULL,
  city VARCHAR(120) NOT NULL,
  state VARCHAR(120) NOT NULL,
  pincode VARCHAR(20) NOT NULL,
  landmark VARCHAR(180),
  delivery_method VARCHAR(80) NOT NULL,
  estimated_delivery VARCHAR(80) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  shipping DECIMAL(12,2) NOT NULL,
  estimated_tax DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  total DECIMAL(12,2) NOT NULL,
  promo_code VARCHAR(80),
  completed_order_id BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_razorpay_attempt_receipt UNIQUE (receipt),
  CONSTRAINT uk_razorpay_attempt_order UNIQUE (razorpay_order_id),
  CONSTRAINT uk_razorpay_attempt_payment UNIQUE (razorpay_payment_id),
  CONSTRAINT uk_razorpay_attempt_completed_order UNIQUE (completed_order_id),
  CONSTRAINT fk_razorpay_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_razorpay_attempt_order_record FOREIGN KEY (completed_order_id) REFERENCES orders(id)
);

CREATE TABLE razorpay_payment_attempt_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  product_id BIGINT NULL,
  product_sku VARCHAR(80) NOT NULL,
  product_name VARCHAR(180) NOT NULL,
  product_image VARCHAR(255),
  size VARCHAR(60),
  color VARCHAR(80),
  unit_price DECIMAL(12,2) NOT NULL,
  qty INT NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_razorpay_attempt_item_attempt FOREIGN KEY (attempt_id) REFERENCES razorpay_payment_attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_razorpay_attempt_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

DELETE FROM orders
WHERE payment_provider = 'RAZORPAY'
  AND payment_status <> 'PAID';
