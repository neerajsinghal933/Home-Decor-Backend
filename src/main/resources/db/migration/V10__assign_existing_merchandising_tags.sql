INSERT INTO product_tags (product_id, tag_id)
SELECT p.id, t.id FROM products p JOIN tags t ON LOWER(t.name) = 'bestseller'
WHERE LOWER(COALESCE(p.badge, '')) IN ('best seller', 'bestseller')
  AND NOT EXISTS (SELECT 1 FROM product_tags pt WHERE pt.product_id = p.id AND pt.tag_id = t.id);

INSERT INTO product_tags (product_id, tag_id)
SELECT p.id, t.id FROM products p JOIN tags t ON LOWER(t.name) = 'new arrival'
WHERE LOWER(COALESCE(p.badge, '')) = 'new'
  AND NOT EXISTS (SELECT 1 FROM product_tags pt WHERE pt.product_id = p.id AND pt.tag_id = t.id);

INSERT INTO product_tags (product_id, tag_id)
SELECT p.id, t.id FROM products p JOIN tags t ON LOWER(t.name) = 'sale'
WHERE LOWER(COALESCE(p.badge, '')) = 'sale'
  AND NOT EXISTS (SELECT 1 FROM product_tags pt WHERE pt.product_id = p.id AND pt.tag_id = t.id);

INSERT INTO product_tags (product_id, tag_id)
SELECT p.id, t.id FROM products p JOIN tags t ON LOWER(t.name) = 'featured'
WHERE p.featured = TRUE
  AND NOT EXISTS (SELECT 1 FROM product_tags pt WHERE pt.product_id = p.id AND pt.tag_id = t.id);
