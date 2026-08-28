# 📦 Product Inventory Management System (ORIO)

An enterprise-grade Product Inventory Management web application built with **Java 17+**, **Spring Boot 3**, **Spring Data JPA**, **SQLite (Persistent by Default)**, **PostgreSQL / MySQL / H2**, a reactive **HTML5 / CSS3 / JavaScript** modular dashboard, containerized with **Docker**, and covered by automated **JUnit 5** test suites.

---

## 🌐 Live Public Demo
- **Live URL**: **[https://9ec43a7f919fc2.lhr.life](https://9ec43a7f919fc2.lhr.life)**
- **Local URL**: `http://localhost:8080`

---

## 🌟 Key Features

1. **Product Catalog & Management**:
   - Create, Read, Update, Delete (CRUD) catalog items with SKU, categories, prices, and descriptions.
   - **Business Rule Enforcement**: Product price cannot be negative (`price >= 0.00`).
   - SKU uniqueness guarantees across all products.

2. **Stock & Inventory Operations**:
   - **Stock-In**: Real-time replenishment with transaction audit logging (`POST /api/inventory/{productId}/stock-in`).
   - **Stock-Out**: Safe inventory dispatch (`POST /api/inventory/{productId}/stock-out`).
   - **Business Rules Enforcement**:
     - Stock cannot become negative (`quantity >= 0`).
     - Stock-out quantity cannot exceed available stock (returns `HTTP 400 Bad Request` with structured `InsufficientStockException`).
   - **Configurable Low-Stock Threshold**: Per-product and global threshold configurations.

3. **Real-Time Low-Stock Monitor**:
   - Instant visual alerts for items whose quantity is at or below the configured reorder threshold.
   - Dynamic deficit calculation (`threshold - current_stock`) and one-click restock workflow.

4. **Analytical SQL Engine (Required SQL Tasks)**:
   - Executes complex queries using `JOIN`, `GROUP BY`, `HAVING`, `ORDER BY`, `COUNT`, and `SUM` across products, categories, inventories, and transaction tables.
   - Interactive SQL Explorer in the Web UI to test queries live with dynamic parameters.

5. **Modular Single-Page Dashboard**:
   - **Themes**: **Neo-Obsidian Dark Mode** (default) and **Frost Light Mode** with a 1-click theme switcher in the sidebar.
   - **Sidebar Navigation**: Dedicated views for Products Catalog, Stock Operations, Low-Stock Alerts, SQL Analytics, and Audit Logs.
   - **Live SQLite Badge**: Displays real-time database engine status and file location.
   - **Toast Notifications**: Micro-animated feedback for successful updates and validation errors.

---

## 🏗️ Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 3.3.4 (Java 17 / Java 21 / Java 25) |
| **ORM / Data Access** | Spring Data JPA / Hibernate 6 (Hibernate Community Dialects) |
| **Default Database** | **SQLite** (`./data/inventory.db` — persistent on hard disk) |
| **Alternative Databases** | PostgreSQL 15+, MySQL 8+, H2 In-Memory (profile-activated) |
| **Frontend** | Semantic HTML5, Vanilla CSS3 (Custom Properties & Glassmorphism), Vanilla JavaScript ES6+ |
| **Testing** | JUnit 5, AssertJ, Spring MockMvc (25/25 tests passing) |
| **Containerization** | Docker, Docker Compose (Multi-stage Eclipse Temurin 17 Alpine) |
| **Build Tool** | Apache Maven 3.9+ |

---

## 📁 Database Schema & SQL Architecture

### Table Definitions & Constraints
- **`categories`**: `id (PK)`, `name (UNIQUE)`, `description`, `created_at`, `updated_at`.
- **`products`**: `id (PK)`, `sku (UNIQUE)`, `name`, `description`, `price (CHECK >= 0)`, `category_id (FK)`, timestamps.
- **`inventories`**: `id (PK)`, `product_id (FK, UNIQUE)`, `quantity (CHECK >= 0)`, `low_stock_threshold (CHECK >= 0)`, `last_restocked_at`.
- **`stock_transactions`**: `id (PK)`, `product_id (FK)`, `transaction_type (STOCK_IN / STOCK_OUT)`, `quantity (CHECK > 0)`, `previous_stock`, `new_stock`, `notes`, `transaction_time`.

### Required Analytical SQL Tasks (`src/main/resources/db/queries.sql`)

#### 1. Products with Available Stock
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

#### 2. Low-Stock Products & Deficit Analysis
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

#### 3. Stock by Category (Aggregation with GROUP BY & SUM)
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

#### 5. Categories Containing More Than a Specified Number of Products (HAVING Clause)
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
| `DELETE` | `/api/products/{id}` | Delete a product and its inventory |

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

## 🐳 Docker Deployment

The application includes a production-ready, multi-stage **`Dockerfile`** (~180MB) and **`docker-compose.yml`** with persistent volume mounts.

### 1. Run with Docker Compose (Single Command):
```bash
docker compose up --build -d
```

### 2. Run with Docker CLI:
```bash
# Build Docker image
docker build -t orio-inventory:latest .

# Run with persistent volume
docker run -d -p 8080:8080 -v ${PWD}/data:/app/data --name orio-app orio-inventory:latest
```

---

## ☁️ Free Cloud Deployment Guides

### Option A: Render.com (100% Free)
1. Push repository to GitHub.
2. Sign in to [Render.com](https://render.com) and click **"New +"** &rarr; **"Web Service"**.
3. Select your GitHub repository.
4. Render will automatically detect the **`Dockerfile`**. Select the **Free** tier and click **"Deploy"**.

### Option B: Koyeb.com (Free Tier)
1. Sign in to [Koyeb.com](https://koyeb.com).
2. Create a new service from your GitHub repo using the `Dockerfile` on the free nano tier.

---

## 🏃 Local Development & Execution

### 1. Run Application
```powershell
# In PowerShell:
.\run.ps1

# Or in Command Prompt:
run.bat
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

### 2. Run Automated Test Suite
```powershell
.\test.ps1
```
*(Runs 25 automated JUnit 5 tests covering business logic, controller endpoints, validation rejections, and SQL query aggregations).*