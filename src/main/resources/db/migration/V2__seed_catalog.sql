INSERT INTO categories (slug, name, image, display_order) VALUES
('all', 'All', 'assets/crops/cat-wall.png', 0),
('wall-decor', 'Wall Decor', 'assets/crops/cat-wall.png', 1),
('vases', 'Vases', 'assets/crops/cat-vase.png', 2),
('planters', 'Planters', 'assets/crops/cat-planter.png', 3),
('sculptures', 'Sculptures', 'assets/crops/cat-sculpture.png', 4),
('table-decor', 'Table Decor', 'assets/crops/cat-table.png', 5),
('lamps-lighting', 'Lamps & Lighting', 'assets/crops/cat-lamp.png', 6),
('home-accessories', 'Home Accessories', 'assets/crops/cat-home.png', 7),
('decorative-objects', 'Decorative Objects', 'assets/crops/cat-object.png', 8);

INSERT INTO products
(sku, slug, name, description, category_id, price, compare_at_price, badge, color, material, dimensions, stock, review_count, status, featured, primary_image)
VALUES
('IND-VASE-001', 'textured-ceramic-vase', 'Textured Ceramic Vase', 'A handcrafted ceramic vase with a warm textured sand finish.', (SELECT id FROM categories WHERE slug='vases'), 2499, 3499, 'New', 'Sand', 'Ceramic', 'Height: 28 cm | Diameter: 18 cm | Weight: 1.2 kg', 18, 12, 'ACTIVE', TRUE, 'assets/crops/prod-vase.png'),
('IND-BOWL-001', 'stone-finish-bowl', 'Stone Finish Bowl', 'A sculptural table bowl with a rich stone finish.', (SELECT id FROM categories WHERE slug='table-decor'), 1899, NULL, 'Best Seller', 'Brown', 'Stoneware', 'Diameter: 24 cm | Height: 9 cm', 24, 28, 'ACTIVE', TRUE, 'assets/crops/prod-bowl.png'),
('IND-SCULPT-001', 'embrace-sculpture', 'Embrace Sculpture', 'A warm walnut-toned accent sculpture for shelves and consoles.', (SELECT id FROM categories WHERE slug='sculptures'), 3499, NULL, 'New', 'Walnut', 'Resin', 'Height: 31 cm | Width: 16 cm', 9, 15, 'ACTIVE', TRUE, 'assets/crops/prod-sculpture.png'),
('IND-PLANTER-001', 'ribbed-planter', 'Ribbed Planter', 'A subtle ribbed planter for indoor greens.', (SELECT id FROM categories WHERE slug='planters'), 1799, NULL, '', 'Beige', 'Ceramic', 'Height: 18 cm | Diameter: 20 cm', 16, 10, 'ACTIVE', FALSE, 'assets/crops/prod-planter.png'),
('IND-LAMP-001', 'earth-table-lamp', 'Earth Table Lamp', 'A softly glowing lamp with an earthy contemporary profile.', (SELECT id FROM categories WHERE slug='lamps-lighting'), 4299, 4599, 'Sale', 'Amber', 'Ceramic and linen', 'Height: 42 cm | Shade: 28 cm', 11, 27, 'ACTIVE', TRUE, 'assets/crops/prod-lamp.png'),
('IND-ART-001', 'abstract-wall-art', 'Abstract Wall Art', 'A calm natural-toned artwork for warm modern walls.', (SELECT id FROM categories WHERE slug='wall-decor'), 2999, NULL, '', 'Natural', 'Canvas', '60 cm x 90 cm', 14, 19, 'ACTIVE', FALSE, 'assets/crops/prod-art.png'),
('IND-PLANTER-002', 'olive-planter', 'Olive Planter', 'A best-selling olive planter with a refined matte finish.', (SELECT id FROM categories WHERE slug='planters'), 2199, NULL, 'Best Seller', 'Olive', 'Ceramic', 'Height: 22 cm | Diameter: 24 cm', 21, 34, 'ACTIVE', TRUE, 'assets/crops/prod-olive.png'),
('IND-BOX-001', 'stone-trinket-box', 'Stone Trinket Box', 'A compact decorative box for bedside and console styling.', (SELECT id FROM categories WHERE slug='decorative-objects'), 1499, NULL, '', 'Stone', 'Composite stone', '12 cm x 10 cm x 7 cm', 27, 11, 'ACTIVE', FALSE, 'assets/crops/prod-box.png');
