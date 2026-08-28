-- ==============================================================================
-- PRODUCT INVENTORY MANAGEMENT SYSTEM - REQUIRED ANALYTICAL SQL QUERIES
-- ==============================================================================
-- Concepts Demonstrated: JOIN, GROUP BY, HAVING, ORDER BY, COUNT, SUM
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- QUERY 1: Products with Available Stock
-- Question: Retrieve all products that currently have stock greater than zero,
--           along with category names, SKU, price, and current quantity in descending stock order.
-- ------------------------------------------------------------------------------
SELECT 
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    p.price,
    i.quantity AS available_stock,
    (p.price * i.quantity) AS total_item_value,
    i.last_restocked_at
FROM products p
INNER JOIN categories c ON p.category_id = c.id
INNER JOIN inventories i ON p.id = i.product_id
WHERE i.quantity > 0
ORDER BY i.quantity DESC, p.name ASC;


-- ------------------------------------------------------------------------------
-- QUERY 2: Low-Stock Products
-- Question: Retrieve all products where current stock is less than or equal to 
--           their configured low_stock_threshold (or a global threshold e.g. 10).
-- ------------------------------------------------------------------------------
SELECT 
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    p.price,
    i.quantity AS current_stock,
    i.low_stock_threshold,
    (i.low_stock_threshold - i.quantity) AS deficit,
    CASE 
        WHEN i.quantity = 0 THEN 'OUT_OF_STOCK'
        ELSE 'LOW_STOCK'
    END AS stock_status
FROM products p
INNER JOIN categories c ON p.category_id = c.id
INNER JOIN inventories i ON p.id = i.product_id
WHERE i.quantity <= i.low_stock_threshold
ORDER BY i.quantity ASC, p.name ASC;


-- ------------------------------------------------------------------------------
-- QUERY 3: Stock by Category
-- Question: Aggregate total products, total stock quantity, and average stock per product
--           grouped by category, ordered from highest stock to lowest.
-- ------------------------------------------------------------------------------
SELECT 
    c.id AS category_id,
    c.name AS category_name,
    COUNT(p.id) AS total_products,
    COALESCE(SUM(i.quantity), 0) AS total_stock_quantity,
    COALESCE(AVG(i.quantity), 0.0) AS avg_stock_per_product,
    COALESCE(SUM(p.price * i.quantity), 0.00) AS category_inventory_valuation
FROM categories c
LEFT JOIN products p ON c.id = p.category_id
LEFT JOIN inventories i ON p.id = i.product_id
GROUP BY c.id, c.name
ORDER BY total_stock_quantity DESC, c.name ASC;


-- ------------------------------------------------------------------------------
-- QUERY 4: Total Inventory Value
-- Question: Calculate overall aggregate inventory metrics across the entire warehouse:
--           Total catalog products, total units stored, minimum unit price, maximum unit price,
--           and cumulative total monetary value of all inventory (SUM(price * quantity)).
-- ------------------------------------------------------------------------------
SELECT 
    COUNT(p.id) AS total_distinct_products,
    SUM(i.quantity) AS total_units_in_stock,
    MIN(p.price) AS min_product_price,
    MAX(p.price) AS max_product_price,
    AVG(p.price) AS avg_product_price,
    SUM(p.price * i.quantity) AS total_inventory_valuation
FROM products p
INNER JOIN inventories i ON p.id = i.product_id;


-- ------------------------------------------------------------------------------
-- QUERY 5: Categories Containing More Than a Specified Number of Products
-- Question: Find categories containing more than N products (e.g. min_count = 2 or 3)
--           using GROUP BY and HAVING with COUNT and SUM aggregations.
-- ------------------------------------------------------------------------------
SELECT 
    c.id AS category_id,
    c.name AS category_name,
    COUNT(p.id) AS product_count,
    SUM(i.quantity) AS total_units_in_category,
    SUM(p.price * i.quantity) AS category_valuation
FROM categories c
INNER JOIN products p ON c.id = p.category_id
INNER JOIN inventories i ON p.id = i.product_id
GROUP BY c.id, c.name
HAVING COUNT(p.id) >= 3
ORDER BY product_count DESC, category_valuation DESC;
