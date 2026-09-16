CREATE TABLE tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE product_tags (
  product_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (product_id, tag_id),
  CONSTRAINT fk_product_tags_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_product_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE RESTRICT
);

CREATE INDEX idx_product_tags_tag ON product_tags(tag_id);
CREATE INDEX idx_products_catalog_listing ON products(status, category_id, price, stock);
CREATE INDEX idx_products_catalog_name ON products(name);

INSERT INTO tags (name, active) SELECT 'New Arrival', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'new arrival');
INSERT INTO tags (name, active) SELECT 'Bestseller', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'bestseller');
INSERT INTO tags (name, active) SELECT 'Featured', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'featured');
INSERT INTO tags (name, active) SELECT 'Sale', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'sale');
INSERT INTO tags (name, active) SELECT 'Limited Edition', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'limited edition');
INSERT INTO tags (name, active) SELECT 'Handmade', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'handmade');
INSERT INTO tags (name, active) SELECT 'Sustainable', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'sustainable');
INSERT INTO tags (name, active) SELECT 'Premium', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'premium');
INSERT INTO tags (name, active) SELECT 'Customizable', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'customizable');
INSERT INTO tags (name, active) SELECT 'Exclusive', TRUE WHERE NOT EXISTS (SELECT 1 FROM tags WHERE LOWER(name) = 'exclusive');
