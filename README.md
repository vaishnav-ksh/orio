# 📦 Product Inventory Management System (ORIO)

An enterprise-grade Product Inventory Management web application built with **Java 17+**, **Spring Boot 3**, **Spring Data JPA**, supporting **PostgreSQL / MySQL / H2**, a reactive **HTML5 / CSS3 / JavaScript** frontend, and automated **JUnit 5** test suites.

---

## 🌟 Key Features

1. **Product Management**:
   - Create, Read, Update, Delete (CRUD) catalog items with SKU, categories, prices, and descriptions.
   - **Business Rule Enforcement**: Product price cannot be negative (`price >= 0.00`).
   - SKU uniqueness guarantees across all products.

2. **Stock & Inventory Operations**:
   - **Stock-In**: Real-time replenishment with transaction audit logging (`POST /api/inventory/{productId}/stock-in`).
   - **Stock-Out**: Safe inventory dispatch (`POST /api/inventory/{productId}/stock-out`).
   - **Business Rules Enforcement**:
     - Stock cannot become negative (`quantity >= 0`).
     - Stock-out quantity cannot exceed available stock (returns `HTTP 400 Bad Request` with custom `InsufficientStockException`).
   - **Configurable Low-Stock Threshold**: Per-product and global threshold configurations.

3. **Real-Time Low-Stock Monitor**:
   - Instant visual alerts for items whose quantity is at or below the configured reorder threshold.
   - Dynamic deficit calculation and one-click restock workflow.

4. **Analytical SQL Engine**:
   - Executes complex queries using `JOIN`, `GROUP BY`, `HAVING`, `ORDER BY`, `COUNT`, and `SUM` across products, categories, inventories, and transaction tables.
   - Live web interface explorer to run and inspect query results interactively.

5. **Modern Single-Page UI**:
   - Dark-mode glassmorphic interface with Google Fonts (*Plus Jakarta Sans*, *Outfit*, *JetBrains Mono*).
   - Asynchronous API communication (`async/await fetch`) with live DOM updates (no full page reload).
   - Floating animated toast notifications and real-time validation error handling.

---

## 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 3.3.4 (Java 17 / Java 21 / Java 25) |
| **ORM / Data Access** | Spring Data JPA / Hibernate 6 |
| **Databases** | PostgreSQL 15+, MySQL 8+, H2 Database (In-Memory default) |
| **Frontend** | Semantic HTML5, Vanilla CSS3 (Custom Properties & Glassmorphism), Vanilla JavaScript ES6+ |
| **Testing** | JUnit 5, AssertJ, Spring MockMvc |
| **Build Tool** | Apache Maven 3.9+ |

---

## 📐 Database Schema & SQL Architecture

### Table Definitions & Constraints
- **`categories`**: `id (PK)`, `name (UNIQUE)`, `description`, `created_at`, `updated_at`.
- **`products`**: `id (PK)`, `sku (UNIQUE)`, `name`, `description`, `price (CHECK >= 0)`, `category_id (FK)`, timestamps.
- **`inventories`**: `id (PK)`, `product_id (FK, UNIQUE)`, `quantity (CHECK >= 0)`, `low_stock_threshold (CHECK >= 0)`, `last_restocked_at`.
- **`stock_transactions`**: `id (PK)`, `product_id (FK)`, `transaction_type (STOCK_IN / STOCK_OUT)`, `quantity (CHECK > 0)`, `previous_stock`, `new_stock`, `notes`, `transaction_time`.

### Required Analytical Queries (`src/main/resources/db/queries.sql`)

#### 1. Products with Available Stock
```sql
SELECT 
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    p.price,
    i.quantity AS available_stock,
    (p.price * i.quantity) AS total_item_value
FROM products p
INNER JOIN categories c ON p.category_id = c.id
INNER JOIN inventories i ON p.id = i.product_id
WHERE i.quantity > 0
ORDER BY i.quantity DESC, p.name ASC;
```

#### 2. Low-Stock Products
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

#### 3. Stock by Category (Aggregation)
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

#### 4. Total Inventory Value (Warehouse Valuation)
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

#### 5. Categories Containing More Than a Specified Number of Products
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

## 🚀 REST API Reference

### Products API
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/products` | Get products (supports `search`, `categoryId`, `inStockOnly` params) |
| `GET` | `/api/products/{id}` | Get product details by ID |
| `POST` | `/api/products` | Create a new product (validates non-negative price, SKU) |
| `PUT` | `/api/products/{id}` | Update product details |
| `DELETE` | `/api/products/{id}` | Delete a product |

### Inventory API
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/inventory` | Retrieve inventory list with stock levels & valuation |
| `GET` | `/api/inventory/{productId}` | Retrieve single product inventory record |
| `POST` | `/api/inventory/{productId}/stock-in` | Add units to stock (records audit transaction) |
| `POST` | `/api/inventory/{productId}/stock-out` | Dispatch units from stock (validates quantity <= available) |
| `GET` | `/api/inventory/low-stock` | Retrieve products at or below threshold (supports `?threshold=N`) |
| `PATCH` | `/api/inventory/{productId}/threshold` | Configure alert threshold for product (`?threshold=N`) |
| `GET` | `/api/inventory/transactions` | Retrieve transaction audit history log |

### Analytics API
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/summary` | Global warehouse KPIs & valuation (Query 4) |
| `GET` | `/api/analytics/stock-by-category` | Stock aggregated by category (Query 3) |
| `GET` | `/api/analytics/available-stock` | Available stock items descending (Query 1) |
| `GET` | `/api/analytics/low-stock-products` | Low stock items ascending (Query 2) |
| `GET` | `/api/analytics/category-product-count` | Categories with &ge; N products (Query 5) |

---

## 🧪 Testing

The test suite includes 25 unit and integration tests covering all critical business rules:
- **`ProductValidationTest`**: Verifies rejection of negative price, negative initial stock, and duplicate SKU.
- **`InventoryServiceTest`**: Verifies stock-in increments, stock-out decrements, rejection of excessive stock-out (`InsufficientStockException`), zero stock transitions, and threshold updates.
- **`ProductControllerTest`**: MockMvc tests for product REST APIs and validation error responses.
- **`InventoryControllerTest`**: MockMvc tests for stock-in, stock-out, 400 Bad Request error payloads, and low-stock queries.
- **`AnalyticsServiceTest`**: Tests all 5 analytical SQL queries for correct aggregation and order.

### Running Tests:
```bash
mvn test
```

---

## 🏃 Running the Application

### 1. Prerequisites
- Java 17 or higher
- Maven 3.8+

### 2. Start Application
```bash
mvn spring-boot:run
```
*(Runs with in-memory H2 database and pre-seeded sample data out of the box)*

### 3. PostgreSQL Mode (Optional)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### 4. Access the Application
- **Web UI**: [http://localhost:8080](http://localhost:8080)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:inventorydb`, User: `sa`, Password: empty)