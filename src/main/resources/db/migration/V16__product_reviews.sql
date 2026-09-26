CREATE TABLE product_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  title VARCHAR(140) NOT NULL,
  body VARCHAR(4000) NOT NULL,
  image_url VARCHAR(500),
  verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT uk_product_reviews_product_user UNIQUE (product_id, user_id),
  CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_reviews_product_created ON product_reviews(product_id, created_at);
CREATE INDEX idx_product_reviews_product_rating ON product_reviews(product_id, rating);

-- Existing catalogue counts were presentation placeholders rather than customer submissions.
UPDATE products SET rating = 0.0, review_count = 0;
