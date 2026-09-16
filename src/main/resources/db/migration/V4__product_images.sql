CREATE TABLE product_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  alt_text VARCHAR(180),
  display_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_order ON product_images (product_id, display_order);

INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, primary_image, name, 0
FROM products
WHERE primary_image IS NOT NULL;

INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'assets/crops/product-main.png', CONCAT(name, ' detail'), 1
FROM products
WHERE slug = 'textured-ceramic-vase';

INSERT INTO product_images (product_id, url, alt_text, display_order)
SELECT id, 'assets/crops/detail-banner.png', CONCAT(name, ' lifestyle'), 2
FROM products
WHERE slug = 'textured-ceramic-vase';
