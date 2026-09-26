CREATE TABLE order_service_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  request_type VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  previous_order_status VARCHAR(40) NOT NULL,
  reason VARCHAR(300) NOT NULL,
  details VARCHAR(1000),
  admin_note VARCHAR(1000),
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at TIMESTAMP NULL,
  reviewed_by_user_id BIGINT NULL,
  INDEX idx_order_requests_order (order_id),
  INDEX idx_order_requests_type_status (request_type, status),
  CONSTRAINT fk_order_requests_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_requests_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id)
);

CREATE TABLE refund_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  service_request_id BIGINT NOT NULL,
  razorpay_payment_id VARCHAR(120) NOT NULL,
  razorpay_refund_id VARCHAR(120),
  idempotency_key VARCHAR(80) NOT NULL,
  receipt VARCHAR(80) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'INR',
  status VARCHAR(30) NOT NULL,
  failure_reason VARCHAR(500),
  refund_reference VARCHAR(160),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  initiated_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_refunds_service_request UNIQUE (service_request_id),
  CONSTRAINT uk_refunds_gateway_id UNIQUE (razorpay_refund_id),
  CONSTRAINT uk_refunds_idempotency UNIQUE (idempotency_key),
  CONSTRAINT uk_refunds_receipt UNIQUE (receipt),
  INDEX idx_refunds_order (order_id),
  INDEX idx_refunds_payment (razorpay_payment_id),
  CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_refunds_request FOREIGN KEY (service_request_id) REFERENCES order_service_requests(id)
);

CREATE TABLE order_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  previous_status VARCHAR(40),
  new_status VARCHAR(40) NOT NULL,
  actor_type VARCHAR(30) NOT NULL,
  reference_type VARCHAR(40),
  reference_id VARCHAR(120),
  note VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order_history_order (order_id, created_at),
  CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE transactional_email_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_key VARCHAR(180) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  recipient VARCHAR(180) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  delivery_status VARCHAR(30) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL,
  CONSTRAINT uk_transactional_email_event_key UNIQUE (event_key)
);
