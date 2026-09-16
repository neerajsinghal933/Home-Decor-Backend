ALTER TABLE orders ADD COLUMN guest_session_id VARCHAR(120);

CREATE INDEX idx_orders_guest_session ON orders(guest_session_id);
