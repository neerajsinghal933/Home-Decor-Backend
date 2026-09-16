ALTER TABLE products
  ADD COLUMN display_order INT NOT NULL DEFAULT 0;

UPDATE products SET display_order = id WHERE display_order = 0;

CREATE INDEX idx_products_display_order ON products(display_order);

ALTER TABLE orders ADD COLUMN razorpay_order_id VARCHAR(120);
ALTER TABLE orders ADD COLUMN razorpay_payment_id VARCHAR(120);
ALTER TABLE orders ADD COLUMN razorpay_signature VARCHAR(255);

CREATE INDEX idx_orders_razorpay_order ON orders(razorpay_order_id);
