# 📖 Product Inventory Management System (ORIO) — Technical Documentation

---

## 1. 🏛️ System Architecture

ORIO is built on an enterprise multi-tier architecture following clean coding principles, domain-driven design, and strict separation of concerns.

```
┌────────────────────────────────────────────────────────┐
│             Frontend Presentation Layer                │
│    (HTML5 + Vanilla CSS3 Glassmorphism + ES6+ JS)     │
└──────────────────────────┬─────────────────────────────┘
                           │ Asynchronous REST (JSON)
                           ▼
┌────────────────────────────────────────────────────────┐
│            REST API Controller Layer                   │
│   (ProductController, InventoryController,             │
│    AnalyticsController, CategoryController)            │
└──────────────────────────┬─────────────────────────────┘
                           │ DTOs & Validation
                           ▼
┌────────────────────────────────────────────────────────┐
│              Business Service Layer                    │
│   (ProductService, InventoryService,                   │
│    AnalyticsService, CategoryService)                  │
└──────────────────────────┬─────────────────────────────┘
                           │ Transactional Entities
                           ▼
┌────────────────────────────────────────────────────────┐
│          Data Access / Repository Layer                │
│   (ProductRepository, InventoryRepository,             │
│    StockTransactionRepository, CategoryRepository)     │
└──────────────────────────┬─────────────────────────────┘
                           │ Spring Data JPA / Hibernate
                           ▼
┌────────────────────────────────────────────────────────┐
│               Relational Database                      │
│   (SQLite ./data/inventory.db | PostgreSQL | MySQL)   │
└────────────────────────────────────────────────────────┘
```

---

## 2. 🗄️ Database Schema & Data Dictionary

### Entity Relationship Model

```mermaid
erDiagram
    CATEGORIES ||--o{ PRODUCTS : "contains"
    PRODUCTS ||--|| INVENTORIES : "has"
    PRODUCTS ||--o{ STOCK_TRANSACTIONS : "logs"

    CATEGORIES {
        bigint id PK
        varchar(100) name UK
        varchar(255) description
        datetime created_at
        datetime updated_at
    }

    PRODUCTS {
        bigint id PK
        varchar(50) sku UK
        varchar(150) name
        text description
        decimal(12_2) price "CHECK (price >= 0)"
        bigint category_id FK
        datetime created_at
        datetime updated_at
    }

    INVENTORIES {
        bigint id PK
        bigint product_id FK_UK
        int quantity "CHECK (quantity >= 0)"
        int low_stock_threshold "CHECK (low_stock_threshold >= 0)"
        datetime last_restocked_at
        datetime updated_at
    }

    STOCK_TRANSACTIONS {
        bigint id PK
        bigint product_id FK
        varchar(20) transaction_type "STOCK_IN | STOCK_OUT"
        int quantity "CHECK (quantity > 0)"
        int previous_stock
        int new_stock
        varchar(255) notes
        datetime transaction_time
    }
```

### Table DDL & Constraints (`src/main/resources/db/schema.sql`)
- **`CHECK (price >= 0.00)`**: Guarantees at database level that product price is never negative.
- **`CHECK (quantity >= 0)`**: Enforces non-negative stock quantity.
- **`CHECK (low_stock_threshold >= 0)`**: Ensures valid alert boundaries.
- **`UNIQUE (sku)`**: Ensures global SKU uniqueness across product catalogs.
- **Performance Indexes**:
  - `idx_products_category` on `products(category_id)`
  - `idx_products_sku` on `products(sku)`
  - `idx_inventories_product` on `inventories(product_id)`
  - `idx_inventories_quantity` on `inventories(quantity)`
  - `idx_transactions_product` on `stock_transactions(product_id)`
  - `idx_transactions_time` on `stock_transactions(transaction_time)`

---

## 3. 💼 Business Rules & Validation Engine

| Rule | Enforcement Layers | Failure Handling |
|---|---|---|
| **Product price cannot be negative** | • Java Bean Validation `@DecimalMin("0.00")`<br>• Service check `price.compareTo(ZERO) >= 0`<br>• SQL DDL `CHECK (price >= 0)` | Throws `InvalidOperationException` &rarr; `HTTP 400 Bad Request` |
| **Stock cannot become negative** | • Java Bean Validation `@Min(0)`<br>• Inventory Service state check<br>• SQL DDL `CHECK (quantity >= 0)` | Rejects mutation, returns error payload |
| **Stock-out cannot exceed available stock** | • `InventoryService.stockOut()` verification: `if (requested > available)` | Throws `InsufficientStockException(productId, available, requested)` &rarr; `HTTP 400 Bad Request` |
| **Configurable low-stock threshold** | • Per-product customizable threshold in `inventories`<br>• Global fallback via `app.inventory.default-low-stock-threshold: 10` | Dynamic threshold filtering across queries |

---

## 4. 📊 Analytical SQL Queries Breakdown

### Query 1: Products with Available Stock (`JOIN`, `ORDER BY`)
- **Purpose**: Identifies catalog products with available inventory, sorted by highest availability.
```sql
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
```

### Query 2: Low-Stock Products & Deficit Analysis (`JOIN`, `FILTER`, `CASE`)
- **Purpose**: Filters items requiring replenishment with remaining deficit.
```sql
SELECT 
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    p.price,
    i.quantity AS current_stock,
    i.low_stock_threshold,
    (i.low_stock_threshold - i.quantity) AS deficit,
    CASE WHEN i.quantity = 0 THEN 'OUT_OF_STOCK' ELSE 'LOW_STOCK' END AS stock_status
FROM products p
INNER JOIN categories c ON p.category_id = c.id
INNER JOIN inventories i ON p.id = i.product_id
WHERE i.quantity <= i.low_stock_threshold
ORDER BY i.quantity ASC, p.name ASC;
```

### Query 3: Stock by Category (`GROUP BY`, `SUM`, `COUNT`, `AVG`)
- **Purpose**: Aggregates inventory volume and valuation metrics across each category.
```sql
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
ORDER BY total_stock_quantity DESC;
```

### Query 4: Total Inventory Value (`SUM`, `MIN`, `MAX`, `AVG`)
- **Purpose**: Calculates overall warehouse valuation and statistical price metrics.
```sql
SELECT 
    COUNT(p.id) AS total_distinct_products,
    SUM(i.quantity) AS total_units_in_stock,
    MIN(p.price) AS min_product_price,
    MAX(p.price) AS max_product_price,
    AVG(p.price) AS avg_product_price,
    SUM(p.price * i.quantity) AS total_inventory_valuation
FROM products p
INNER JOIN inventories i ON p.id = i.product_id;
```

### Query 5: Categories Containing More Than N Products (`HAVING`, `GROUP BY`)
- **Purpose**: Filters categories with &ge; N products using the `HAVING` clause.
```sql
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
HAVING COUNT(p.id) >= :minCount
ORDER BY product_count DESC;
```

---

## 5. 🔌 Complete REST API Specification

### Products Endpoints
- `GET /api/products`: Retrieve all products with search, category filtering, and in-stock toggle.
- `GET /api/products/{id}`: Retrieve single product by ID.
- `POST /api/products`: Create a new product.
  ```json
  {
    "sku": "ELEC-MACBOOK-16",
    "name": "MacBook Pro 16 M3 Max",
    "description": "36GB RAM, 1TB SSD",
    "price": 3499.00,
    "categoryId": 1,
    "initialStock": 20,
    "lowStockThreshold": 5
  }
  ```
- `PUT /api/products/{id}`: Update an existing product.
- `DELETE /api/products/{id}`: Delete product and associated inventory.

### Inventory Endpoints
- `GET /api/inventory`: Full list of inventory records with status and valuations.
- `POST /api/inventory/{productId}/stock-in`:
  ```json
  {
    "quantity": 10,
    "notes": "Warehouse replenishment PO #8291"
  }
  ```
- `POST /api/inventory/{productId}/stock-out`:
  ```json
  {
    "quantity": 3,
    "notes": "Customer order #4928"
  }
  ```
- `GET /api/inventory/low-stock?threshold=10`: Retrieve items at or below alert threshold.
- `PATCH /api/inventory/{productId}/threshold?threshold=15`: Update alert threshold.
- `GET /api/inventory/transactions`: Retrieve complete audit transaction log.

### Analytics Endpoints
- `GET /api/analytics/summary`: Global warehouse valuation metrics.
- `GET /api/analytics/stock-by-category`: Category aggregation breakdown.
- `GET /api/analytics/available-stock`: Available stock items descending.
- `GET /api/analytics/low-stock-products`: Low stock items ascending.
- `GET /api/analytics/category-product-count?minCount=2`: Categories with &ge; N products.

---

## 6. 🎨 Frontend Modular UI Architecture

- **`index.html`**: Semantic layout containing Sidebar, Top Header, KPI Grid, and Tab Modules.
- **`styles.css`**: CSS Custom Properties (`--bg-app`, `--primary-gradient`, `--glass-blur`), responsive grid layout, and Dark/Light mode theme system.
- **`api.js`**: Reusable asynchronous HTTP client with unified error interceptor.
- **`app.js`**: Modular Single-Page Application (SPA) controller with reactive DOM manipulation, real-time input validations, and modal dialog managers.

---

## 7. 🧪 Testing & Quality Assurance

- **Frameworks**: JUnit 5, Spring Boot Test, MockMvc, AssertJ.
- **Total Test Cases**: **25 tests (0 failures, 0 errors)**.
- **Test Matrix**:
  1. `ProductValidationTest`: Negative price rejection, negative stock rejection, SKU uniqueness.
  2. `InventoryServiceTest`: Atomic stock-in, atomic stock-out, excessive stock-out rejection, low-stock threshold detection.
  3. `ProductControllerTest`: REST API endpoints and HTTP status code mappings.
  4. `InventoryControllerTest`: MockMvc endpoints and JSON error structure verification.
  5. `AnalyticsServiceTest`: Analytical SQL query aggregations and order guarantees.

---

## 8. 🚢 DevOps & Deployment

- **Docker Container**: Multi-stage build (`maven:3.9.9-eclipse-temurin-17-alpine` &rarr; `eclipse-temurin:17-jre-alpine`).
- **Disk Persistence**: Volume mount `-v ./data:/app/data` ensures zero data loss.
- **One-Click Commands**:
  - `docker compose up --build -d` (Docker startup)
  - `.\run.ps1` (Local PowerShell startup)
  - `.\test.ps1` (Automated test runner)
