ALTER TABLE orders
  ADD COLUMN promo_code VARCHAR(80) NULL;

ALTER TABLE orders
  ADD COLUMN discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE TABLE promo_codes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(80) NOT NULL,
  description VARCHAR(255),
  discount_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(12,2) NOT NULL,
  minimum_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_promo_codes_code UNIQUE (code)
);

INSERT INTO promo_codes (code, description, discount_type, discount_value, minimum_order_amount, active)
VALUES ('NEST10', 'Introductory 10% discount', 'PERCENT', 10.00, 0.00, TRUE);
