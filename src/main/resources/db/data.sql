-- ==============================================================================
-- PRODUCT INVENTORY MANAGEMENT SYSTEM - SAMPLE SEED DATA
-- ==============================================================================

-- 1. Insert Categories
INSERT INTO categories (id, name, description) VALUES
(1, 'Electronics', 'Smartphones, laptops, accessories and consumer electronics'),
(2, 'Furniture', 'Ergonomic chairs, desks, bookshelves, and home office furniture'),
(3, 'Apparel', 'Men and women clothing, footwear, and wearable fashion'),
(4, 'Groceries', 'Packaged food items, organic grains, beverages and snacks'),
(5, 'Stationery', 'Office supplies, premium notebooks, writing instruments and planners');

-- 2. Insert Products
INSERT INTO products (id, sku, name, description, price, category_id) VALUES
-- Electronics
(1, 'ELEC-MBP-14', 'MacBook Pro 14" M3', 'Apple MacBook Pro 14-inch with 18GB Unified Memory and 512GB SSD', 1999.00, 1),
(2, 'ELEC-SNY-WH1000', 'Sony WH-1000XM5 Headphones', 'Industry-leading noise canceling wireless over-ear headphones', 349.99, 1),
(3, 'ELEC-LOG-MX3', 'Logitech MX Master 3S', 'Wireless ergonomic performance mouse with quiet clicks', 99.99, 1),
(4, 'ELEC-KBD-MECH', 'Keychron Q1 Pro Mechanical Keyboard', 'Wireless Custom Mechanical Keyboard with Hot-Swappable Switches', 199.00, 1),

-- Furniture
(5, 'FURN-CHAIR-ERG', 'Ergohuman High-Back Ergonomic Chair', 'Breathable mesh executive task chair with lumbar support', 629.50, 2),
(6, 'FURN-DESK-STD', 'ApexDesk Motorized Standing Desk', 'Electric height adjustable 60-inch dual-motor bamboo desk', 549.00, 2),
(7, 'FURN-LAMP-LED', 'BenQ ScreenBar Monitor Light', 'Auto-dimming e-Reading LED monitor light bar with dial control', 139.00, 2),

-- Apparel
(8, 'APP-HOODIE-BLK', 'Classic Organic Cotton Hoodie', 'Heavyweight 450 GSM organic French terry cotton hoodie in jet black', 78.00, 3),
(9, 'APP-JCKT-WTR', 'Alpine Waterproof Shell Jacket', '3-layer Gore-Tex breathable windproof and waterproof mountain shell', 289.00, 3),

-- Groceries
(10, 'GROC-COF-ETH', 'Ethiopian Yirgacheffe Single Origin Coffee Beans', 'Medium roast 1kg specialty grade whole bean arabica coffee', 24.50, 4),
(11, 'GROC-TEA-MTCH', 'Uji Ceremonial Grade Matcha 100g', 'First harvest authentic stone ground Japanese green tea powder', 32.00, 4),

-- Stationery
(12, 'STAT-NOTE-LEU', 'Leuchtturm1917 Hardcover Notebook A5', 'Dotted grid 251 numbered pages fountain-pen friendly notebook', 22.50, 5),
(13, 'STAT-PEN-LAMY', 'Lamy Safari Fountain Pen - Matte Black', 'Timeless ergonomic fountain pen with fine stainless steel nib', 29.90, 5),
(14, 'STAT-ORG-DSK', 'Oak Wood Modular Desk Organizer', 'Solid European white oak modular tray organizer with brass accents', 45.00, 5);

-- 3. Insert Inventories (Some in stock, some low-stock <= 10, some critically out of stock)
INSERT INTO inventories (id, product_id, quantity, low_stock_threshold, last_restocked_at) VALUES
(1, 1, 15, 5, CURRENT_TIMESTAMP),    -- MacBook Pro: 15 (Threshold 5 -> Normal)
(2, 2, 4, 10, CURRENT_TIMESTAMP),    -- Sony Headphones: 4 (Threshold 10 -> Low Stock!)
(3, 3, 42, 10, CURRENT_TIMESTAMP),   -- Logitech MX: 42 (Normal)
(4, 4, 8, 10, CURRENT_TIMESTAMP),    -- Keychron Keyboard: 8 (Threshold 10 -> Low Stock!)
(5, 5, 12, 5, CURRENT_TIMESTAMP),    -- Ergonomic Chair: 12 (Normal)
(6, 6, 2, 5, CURRENT_TIMESTAMP),     -- Standing Desk: 2 (Threshold 5 -> Low Stock!)
(7, 7, 25, 8, CURRENT_TIMESTAMP),    -- Monitor Light: 25 (Normal)
(8, 8, 50, 15, CURRENT_TIMESTAMP),   -- Hoodie: 50 (Normal)
(9, 9, 3, 10, CURRENT_TIMESTAMP),    -- Jacket: 3 (Threshold 10 -> Low Stock!)
(10, 10, 85, 20, CURRENT_TIMESTAMP), -- Coffee: 85 (Normal)
(11, 11, 6, 15, CURRENT_TIMESTAMP),  -- Matcha: 6 (Threshold 15 -> Low Stock!)
(12, 12, 110, 25, CURRENT_TIMESTAMP),-- Notebook: 110 (Normal)
(13, 13, 0, 10, CURRENT_TIMESTAMP),  -- Lamy Pen: 0 (Threshold 10 -> Out of Stock & Low Stock!)
(14, 14, 18, 10, CURRENT_TIMESTAMP); -- Organizer: 18 (Normal)

-- 4. Initial Stock Transactions
INSERT INTO stock_transactions (product_id, transaction_type, quantity, previous_stock, new_stock, notes, transaction_time) VALUES
(1, 'STOCK_IN', 15, 0, 15, 'Initial shipment from manufacturer', CURRENT_TIMESTAMP),
(2, 'STOCK_IN', 20, 0, 20, 'Initial inventory arrival', CURRENT_TIMESTAMP),
(2, 'STOCK_OUT', 16, 20, 4, 'B2B order fulfillment', CURRENT_TIMESTAMP),
(13, 'STOCK_IN', 10, 0, 10, 'Initial batch', CURRENT_TIMESTAMP),
(13, 'STOCK_OUT', 10, 10, 0, 'Flash sale clear-out', CURRENT_TIMESTAMP);
