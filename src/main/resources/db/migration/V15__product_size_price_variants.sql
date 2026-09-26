CREATE TABLE product_size_variants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  size_label VARCHAR(60) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT uk_product_size_variant UNIQUE (product_id, size_label),
  CONSTRAINT fk_product_size_variants_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_size_variants_product ON product_size_variants(product_id);
