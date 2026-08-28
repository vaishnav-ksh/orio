-- ==============================================================================
-- PRODUCT INVENTORY MANAGEMENT SYSTEM - DATABASE SCHEMA DDL
-- Supported on PostgreSQL, MySQL 8+, and H2 Database
-- ==============================================================================

-- 1. DROP EXISTING TABLES (In correct dependency order)
DROP TABLE IF EXISTS stock_transactions;
DROP TABLE IF EXISTS inventories;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;

-- 2. CATEGORIES TABLE
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. PRODUCTS TABLE
-- Business Rule: Product price cannot be negative (CHECK price >= 0)
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- 4. INVENTORIES TABLE
-- Business Rule: Stock cannot become negative (CHECK quantity >= 0)
CREATE TABLE inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 10,
    last_restocked_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_low_stock_threshold CHECK (low_stock_threshold >= 0),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 5. STOCK TRANSACTIONS AUDIT TABLE
CREATE TABLE stock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- 'STOCK_IN' or 'STOCK_OUT'
    quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    notes VARCHAR(255),
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_txn_quantity CHECK (quantity > 0),
    CONSTRAINT fk_txn_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 6. PERFORMANCE INDEXES
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_inventories_product_id ON inventories(product_id);
CREATE INDEX idx_inventories_quantity ON inventories(quantity);
CREATE INDEX idx_transactions_product_id ON stock_transactions(product_id);
CREATE INDEX idx_transactions_time ON stock_transactions(transaction_time);
