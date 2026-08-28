/**
 * ORIO Product Inventory Management - Modular Application Controller
 */

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});

const App = {
    state: {
        products: [],
        categories: [],
        inventory: [],
        lowStockItems: [],
        transactions: [],
        activeTab: 'products-tab',
        activeQuery: 'q1',
        currentEditProductId: null,
        selectedProductForStock: null,
        theme: localStorage.getItem('orio_theme') || 'dark'
    },

    init() {
        this.applyTheme(this.state.theme);
        this.bindEvents();
        this.loadCategories();
        this.loadAllData();
    },

    bindEvents() {
        // Theme Switcher
        document.getElementById('theme-toggle-btn').addEventListener('click', () => {
            const nextTheme = this.state.theme === 'dark' ? 'light' : 'dark';
            this.applyTheme(nextTheme);
        });

        // Mobile Sidebar Toggle
        const mobileToggle = document.getElementById('mobile-sidebar-toggle');
        const sidebar = document.getElementById('sidebar');
        if (mobileToggle && sidebar) {
            mobileToggle.addEventListener('click', () => {
                sidebar.classList.toggle('open');
            });
        }

        // Tab Navigation
        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.getAttribute('data-tab');
                this.switchTab(targetTab);
                if (sidebar.classList.contains('open')) {
                    sidebar.classList.remove('open');
                }
            });
        });

        // Global Refresh & Create Product triggers
        document.getElementById('header-refresh-btn').addEventListener('click', () => {
            this.loadAllData();
            this.showToast('Data refreshed successfully', 'success');
        });

        document.getElementById('btn-open-create-modal').addEventListener('click', () => {
            this.openProductModal();
        });

        // Search and Filters
        const searchInput = document.getElementById('product-search-input');
        const clearSearchBtn = document.getElementById('clear-product-search');
        let searchTimeout = null;

        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            clearSearchBtn.style.display = searchInput.value ? 'inline-block' : 'none';
            searchTimeout = setTimeout(() => {
                this.loadProducts();
            }, 250);
        });

        clearSearchBtn.addEventListener('click', () => {
            searchInput.value = '';
            clearSearchBtn.style.display = 'none';
            this.loadProducts();
        });

        document.getElementById('product-category-filter').addEventListener('change', () => {
            this.loadProducts();
        });

        document.getElementById('filter-in-stock-only').addEventListener('change', () => {
            this.loadProducts();
        });

        // Modals Controls
        document.getElementById('btn-close-product-modal').addEventListener('click', () => this.closeProductModal());
        document.getElementById('btn-cancel-product-modal').addEventListener('click', () => this.closeProductModal());
        document.getElementById('btn-close-stock-op-modal').addEventListener('click', () => this.closeStockOpModal());
        document.getElementById('btn-cancel-stock-op').addEventListener('click', () => this.closeStockOpModal());

        // Form submissions
        document.getElementById('product-form').addEventListener('submit', (e) => this.handleProductFormSubmit(e));
        document.getElementById('stock-op-form').addEventListener('submit', (e) => this.handleStockOpSubmit(e));

        // Low stock threshold filter apply
        document.getElementById('btn-apply-threshold').addEventListener('click', () => {
            const threshold = document.getElementById('global-threshold-input').value;
            this.loadLowStockItems(threshold);
        });

        // SQL Analytics Query Cards
        document.querySelectorAll('.query-card').forEach(card => {
            card.addEventListener('click', () => {
                document.querySelectorAll('.query-card').forEach(c => c.classList.remove('active'));
                card.classList.add('active');
                const queryId = card.getAttribute('data-query');
                this.switchActiveQuery(queryId);
            });
        });

        document.getElementById('btn-run-active-query').addEventListener('click', () => {
            this.executeActiveQuery();
        });
    },

    applyTheme(theme) {
        this.state.theme = theme;
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('orio_theme', theme);
        const themeText = document.getElementById('theme-mode-text');
        if (themeText) {
            themeText.textContent = theme === 'dark' ? 'Dark Mode' : 'Light Mode';
        }
    },

    switchTab(tabId) {
        this.state.activeTab = tabId;
        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
        });
        document.querySelectorAll('.tab-module').forEach(module => {
            module.classList.toggle('active', module.id === tabId);
        });

        const titles = {
            'products-tab': 'Products Catalog',
            'inventory-tab': 'Stock Level Operations',
            'low-stock-tab': 'Low-Stock Alerts & Deficit',
            'analytics-tab': 'SQL Analytics & Intelligence',
            'transactions-tab': 'Stock Audit Logs'
        };
        const titleElem = document.getElementById('page-title-display');
        if (titleElem && titles[tabId]) {
            titleElem.textContent = titles[tabId];
        }

        // Trigger load for active tab
        if (tabId === 'products-tab') this.loadProducts();
        if (tabId === 'inventory-tab') this.loadInventory();
        if (tabId === 'low-stock-tab') this.loadLowStockItems();
        if (tabId === 'analytics-tab') this.executeActiveQuery();
        if (tabId === 'transactions-tab') this.loadTransactions();
    },

    async loadAllData() {
        await Promise.all([
            this.loadKpis(),
            this.loadProducts(),
            this.loadInventory(),
            this.loadLowStockItems(),
            this.loadTransactions()
        ]);
    },

    async loadKpis() {
        try {
            const summary = await API.getAnalyticsSummary();
            if (summary.success && summary.data) {
                const data = summary.data;
                document.getElementById('kpi-total-products').textContent = data.totalDistinctProducts || 0;
                document.getElementById('kpi-total-units').textContent = data.totalUnitsInStock || 0;
                document.getElementById('kpi-low-stock').textContent = (data.lowStockProductCount || 0) + (data.outOfStockProductCount || 0);
                document.getElementById('kpi-total-valuation').textContent = `$${parseFloat(data.totalInventoryValuation || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

                const counterElem = document.getElementById('nav-counter-products');
                if (counterElem) counterElem.textContent = data.totalDistinctProducts || 0;
            }
        } catch (err) {
            console.error('Error loading KPIs:', err);
        }
    },

    async loadCategories() {
        try {
            const res = await API.getCategories();
            if (res.success && res.data) {
                this.state.categories = res.data;
                const filterSelect = document.getElementById('product-category-filter');
                const formSelect = document.getElementById('form-product-category');

                filterSelect.innerHTML = '<option value="">All Categories</option>';
                formSelect.innerHTML = '<option value="">Select Category</option>';

                res.data.forEach(c => {
                    filterSelect.innerHTML += `<option value="${c.id}">${c.name}</option>`;
                    formSelect.innerHTML += `<option value="${c.id}">${c.name}</option>`;
                });
            }
        } catch (err) {
            console.error('Error loading categories:', err);
        }
    },

    async loadProducts() {
        const tbody = document.getElementById('products-table-body');
        try {
            const search = document.getElementById('product-search-input').value;
            const categoryId = document.getElementById('product-category-filter').value;
            const inStockOnly = document.getElementById('filter-in-stock-only').checked;

            const res = await API.getProducts({ search, categoryId, inStockOnly });
            if (res.success && res.data) {
                this.state.products = res.data;
                this.renderProductsTable(res.data);
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell" style="color: var(--rose-500);">Failed to load products: ${err.message}</td></tr>`;
        }
    },

    renderProductsTable(products) {
        const tbody = document.getElementById('products-table-body');
        if (!products || products.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell">No products found matching the criteria.</td></tr>`;
            return;
        }

        tbody.innerHTML = products.map(p => {
            const stock = p.currentStock || 0;
            let statusBadge = '<span class="status-badge status-in-stock">IN STOCK</span>';
            if (stock === 0) {
                statusBadge = '<span class="status-badge status-out-of-stock">OUT OF STOCK</span>';
            } else if (stock <= (p.lowStockThreshold || 10)) {
                statusBadge = '<span class="status-badge status-low-stock">LOW STOCK</span>';
            }

            const itemVal = (parseFloat(p.price || 0) * stock).toFixed(2);

            return `
                <tr>
                    <td><span class="sku-badge">${this.escapeHtml(p.sku)}</span></td>
                    <td>
                        <strong>${this.escapeHtml(p.name)}</strong>
                        ${p.description ? `<div style="font-size:0.75rem; color:var(--text-muted);">${this.escapeHtml(p.description)}</div>` : ''}
                    </td>
                    <td><span class="category-tag">${this.escapeHtml(p.categoryName || 'General')}</span></td>
                    <td><strong>$${parseFloat(p.price).toFixed(2)}</strong></td>
                    <td><strong>${stock}</strong> units</td>
                    <td>$${parseFloat(itemVal).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    <td>${statusBadge}</td>
                    <td class="text-right">
                        <div class="table-actions">
                            <button class="btn btn-secondary btn-sm" onclick="App.openEditProductModal(${p.id})">Edit</button>
                            <button class="btn btn-danger-soft btn-sm" onclick="App.confirmDeleteProduct(${p.id}, '${this.escapeHtml(p.name)}')">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    async loadInventory() {
        const tbody = document.getElementById('inventory-table-body');
        try {
            const res = await API.getInventory();
            if (res.success && res.data) {
                this.state.inventory = res.data;
                this.renderInventoryTable(res.data);
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell" style="color: var(--rose-500);">Failed to load inventory: ${err.message}</td></tr>`;
        }
    },

    renderInventoryTable(inventory) {
        const tbody = document.getElementById('inventory-table-body');
        if (!inventory || inventory.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell">No inventory records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = inventory.map(i => {
            const stock = i.currentStock || 0;
            let statusBadge = '<span class="status-badge status-in-stock">IN STOCK</span>';
            if (stock === 0) {
                statusBadge = '<span class="status-badge status-out-of-stock">OUT OF STOCK</span>';
            } else if (stock <= i.lowStockThreshold) {
                statusBadge = '<span class="status-badge status-low-stock">LOW STOCK</span>';
            }

            const restockTime = i.lastRestockedAt ? new Date(i.lastRestockedAt).toLocaleString() : 'Never';

            return `
                <tr>
                    <td>
                        <strong>${this.escapeHtml(i.productName)}</strong>
                        <div><span class="sku-badge">${this.escapeHtml(i.productSku)}</span></div>
                    </td>
                    <td><span class="category-tag">${this.escapeHtml(i.categoryName || 'General')}</span></td>
                    <td>$${parseFloat(i.productPrice || 0).toFixed(2)}</td>
                    <td><strong style="font-size: 1rem;">${stock}</strong></td>
                    <td>Threshold: ${i.lowStockThreshold}</td>
                    <td>${statusBadge}</td>
                    <td style="font-size:0.75rem; color:var(--text-muted);">${restockTime}</td>
                    <td class="text-right">
                        <div class="table-actions">
                            <button class="btn btn-success-soft btn-sm" onclick="App.openStockOpModal(${i.productId}, '${this.escapeHtml(i.productName)}', '${this.escapeHtml(i.productSku)}', ${stock}, 'STOCK_IN')">+ Stock In</button>
                            <button class="btn btn-danger-soft btn-sm" onclick="App.openStockOpModal(${i.productId}, '${this.escapeHtml(i.productName)}', '${this.escapeHtml(i.productSku)}', ${stock}, 'STOCK_OUT')">- Stock Out</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    },

    async loadLowStockItems(threshold) {
        const tbody = document.getElementById('low-stock-table-body');
        try {
            const res = await API.getLowStockProducts(threshold);
            if (res.success && res.data) {
                this.state.lowStockItems = res.data;
                const alertCount = res.data.length;
                const alertBadge = document.getElementById('low-stock-badge-count');
                if (alertBadge) {
                    alertBadge.textContent = alertCount;
                    alertBadge.classList.toggle('visible', alertCount > 0);
                }
                this.renderLowStockTable(res.data);
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell" style="color: var(--rose-500);">Failed to load alerts: ${err.message}</td></tr>`;
        }
    },

    renderLowStockTable(items) {
        const tbody = document.getElementById('low-stock-table-body');
        if (!items || items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell" style="color: var(--emerald-500); font-weight: 600;">🎉 All inventory levels are healthy! No items below threshold.</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(item => {
            const statusBadge = item.currentStock === 0
                ? '<span class="status-badge status-out-of-stock">OUT OF STOCK</span>'
                : '<span class="status-badge status-low-stock">LOW STOCK</span>';

            return `
                <tr>
                    <td><strong>${this.escapeHtml(item.productName)}</strong></td>
                    <td><span class="sku-badge">${this.escapeHtml(item.sku)}</span></td>
                    <td><span class="category-tag">${this.escapeHtml(item.categoryName || 'General')}</span></td>
                    <td><strong style="color: var(--rose-500);">${item.currentStock}</strong></td>
                    <td>${item.lowStockThreshold}</td>
                    <td><span class="deficit-badge">-${item.deficit} units</span></td>
                    <td>${statusBadge}</td>
                    <td class="text-right">
                        <button class="btn btn-success-soft btn-sm" onclick="App.openStockOpModal(${item.productId}, '${this.escapeHtml(item.productName)}', '${this.escapeHtml(item.sku)}', ${item.currentStock}, 'STOCK_IN')">+ Quick Restock</button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    async loadTransactions() {
        const tbody = document.getElementById('transactions-table-body');
        try {
            const res = await API.getTransactions();
            if (res.success && res.data) {
                this.state.transactions = res.data;
                this.renderTransactionsTable(res.data);
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell" style="color: var(--rose-500);">Failed to load transactions: ${err.message}</td></tr>`;
        }
    },

    renderTransactionsTable(txs) {
        const tbody = document.getElementById('transactions-table-body');
        if (!txs || txs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="table-loader-cell">No transaction records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = txs.map(tx => {
            const isStockIn = tx.transactionType === 'STOCK_IN';
            const typeBadge = isStockIn
                ? '<span class="status-badge status-in-stock">STOCK IN</span>'
                : '<span class="status-badge status-out-of-stock">STOCK OUT</span>';

            const qtyDelta = isStockIn ? `+${tx.quantity}` : `-${tx.quantity}`;
            const timeStr = new Date(tx.transactionTime).toLocaleString();

            return `
                <tr>
                    <td>#${tx.id}</td>
                    <td style="font-size:0.75rem; color:var(--text-muted);">${timeStr}</td>
                    <td><strong>${this.escapeHtml(tx.productName)}</strong></td>
                    <td><span class="sku-badge">${this.escapeHtml(tx.productSku)}</span></td>
                    <td>${typeBadge}</td>
                    <td><strong style="color:${isStockIn ? 'var(--emerald-500)' : 'var(--rose-500)'}">${qtyDelta}</strong></td>
                    <td>${tx.previousStock} &rarr; <strong>${tx.newStock}</strong></td>
                    <td style="font-size:0.8rem; color:var(--text-secondary);">${this.escapeHtml(tx.notes || '—')}</td>
                </tr>
            `;
        }).join('');
    },

    // SQL Analytics View Manager
    switchActiveQuery(queryId) {
        this.state.activeQuery = queryId;
        const q5Container = document.getElementById('q5-param-container');
        q5Container.style.display = (queryId === 'q5') ? 'flex' : 'none';

        const meta = {
            q1: {
                title: 'Query 1: Products with Available Stock',
                desc: 'Retrieves all products with stock > 0, SKU, category, unit price, and total valuation in descending order of availability.',
                sql: `SELECT p.id, p.sku, p.name, c.name AS category, p.price, i.quantity, (p.price * i.quantity) AS total_value\nFROM products p\nJOIN categories c ON p.category_id = c.id\nJOIN inventories i ON p.id = i.product_id\nWHERE i.quantity > 0\nORDER BY i.quantity DESC, p.name ASC;`
            },
            q2: {
                title: 'Query 2: Low-Stock Products & Deficit Analysis',
                desc: 'Filters products where current stock <= low_stock_threshold, computing remaining deficit units.',
                sql: `SELECT p.id, p.sku, p.name, c.name AS category, p.price, i.quantity, i.low_stock_threshold, (i.low_stock_threshold - i.quantity) AS deficit\nFROM products p\nJOIN categories c ON p.category_id = c.id\nJOIN inventories i ON p.id = i.product_id\nWHERE i.quantity <= i.low_stock_threshold\nORDER BY i.quantity ASC;`
            },
            q3: {
                title: 'Query 3: Stock Aggregation by Category',
                desc: 'Aggregates stock totals, product counts, and cumulative inventory valuation grouped by Category.',
                sql: `SELECT c.id, c.name, COUNT(p.id) AS total_products, COALESCE(SUM(i.quantity), 0) AS total_units, COALESCE(SUM(p.price * i.quantity), 0.0) AS valuation\nFROM categories c\nLEFT JOIN products p ON c.id = p.category_id\nLEFT JOIN inventories i ON p.id = i.product_id\nGROUP BY c.id, c.name\nORDER BY total_units DESC;`
            },
            q4: {
                title: 'Query 4: Warehouse Valuation & Key Statistical Aggregates',
                desc: 'Calculates total distinct products, warehouse units, MIN price, MAX price, AVG price, and overall valuation.',
                sql: `SELECT COUNT(p.id) AS total_products, SUM(i.quantity) AS total_units, MIN(p.price) AS min_price, MAX(p.price) AS max_price, AVG(p.price) AS avg_price, SUM(p.price * i.quantity) AS total_valuation\nFROM products p\nJOIN inventories i ON p.id = i.product_id;`
            },
            q5: {
                title: 'Query 5: Categories with Specified Product Count (HAVING Clause)',
                desc: 'Filters categories that contain greater than or equal to N products using the HAVING aggregation clause.',
                sql: `SELECT c.id, c.name, COUNT(p.id) AS product_count, SUM(i.quantity) AS category_units, SUM(p.price * i.quantity) AS category_valuation\nFROM categories c\nJOIN products p ON c.id = p.category_id\nJOIN inventories i ON p.id = i.product_id\nGROUP BY c.id, c.name\nHAVING COUNT(p.id) >= :minCount\nORDER BY product_count DESC;`
            }
        };

        const activeMeta = meta[queryId] || meta.q1;
        document.getElementById('active-query-title').textContent = activeMeta.title;
        document.getElementById('active-query-desc').textContent = activeMeta.desc;
        document.getElementById('active-sql-code').textContent = activeMeta.sql;

        this.executeActiveQuery();
    },

    async executeActiveQuery() {
        const thead = document.getElementById('analytics-results-thead');
        const tbody = document.getElementById('analytics-results-tbody');
        tbody.innerHTML = `<tr><td colspan="6" class="table-loader-cell"><div class="spinner"></div> Executing SQL Query...</td></tr>`;

        try {
            const queryId = this.state.activeQuery;
            if (queryId === 'q1') {
                const res = await API.getAvailableStock();
                thead.innerHTML = `<tr><th>ID</th><th>SKU</th><th>Product Name</th><th>Category</th><th>Unit Price</th><th>Available Units</th><th>Total Value</th></tr>`;
                tbody.innerHTML = res.data.map(p => `
                    <tr>
                        <td>#${p.id}</td>
                        <td><span class="sku-badge">${this.escapeHtml(p.sku)}</span></td>
                        <td><strong>${this.escapeHtml(p.name)}</strong></td>
                        <td><span class="category-tag">${this.escapeHtml(p.categoryName)}</span></td>
                        <td>$${parseFloat(p.price).toFixed(2)}</td>
                        <td><strong style="color:var(--emerald-500);">${p.currentStock}</strong></td>
                        <td>$${(parseFloat(p.price) * p.currentStock).toFixed(2)}</td>
                    </tr>
                `).join('');
            } else if (queryId === 'q2') {
                const res = await API.getLowStockProducts();
                thead.innerHTML = `<tr><th>SKU</th><th>Product Name</th><th>Category</th><th>Current Units</th><th>Threshold</th><th>Deficit</th><th>Status</th></tr>`;
                tbody.innerHTML = res.data.map(item => `
                    <tr>
                        <td><span class="sku-badge">${this.escapeHtml(item.sku)}</span></td>
                        <td><strong>${this.escapeHtml(item.productName)}</strong></td>
                        <td><span class="category-tag">${this.escapeHtml(item.categoryName)}</span></td>
                        <td><strong style="color:var(--rose-500);">${item.currentStock}</strong></td>
                        <td>${item.lowStockThreshold}</td>
                        <td><span class="deficit-badge">-${item.deficit}</span></td>
                        <td>${item.stockStatus}</td>
                    </tr>
                `).join('');
            } else if (queryId === 'q3') {
                const res = await API.getStockByCategory();
                thead.innerHTML = `<tr><th>Category ID</th><th>Category Name</th><th>Total Products (COUNT)</th><th>Total Units (SUM)</th><th>Avg Stock (AVG)</th><th>Category Valuation (SUM P*Q)</th></tr>`;
                tbody.innerHTML = res.data.map(c => `
                    <tr>
                        <td>#${c.categoryId}</td>
                        <td><strong>${this.escapeHtml(c.categoryName)}</strong></td>
                        <td>${c.totalProducts}</td>
                        <td><strong style="color:var(--cyan-500);">${c.totalStockQuantity}</strong></td>
                        <td>${parseFloat(c.avgStockPerProduct).toFixed(1)}</td>
                        <td>$${parseFloat(c.categoryInventoryValuation).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    </tr>
                `).join('');
            } else if (queryId === 'q4') {
                const res = await API.getAnalyticsSummary();
                const d = res.data;
                thead.innerHTML = `<tr><th>Total Distinct Products</th><th>Total Warehouse Units</th><th>Min Price</th><th>Max Price</th><th>Avg Price</th><th>Total Inventory Valuation</th></tr>`;
                tbody.innerHTML = `
                    <tr>
                        <td><strong>${d.totalDistinctProducts}</strong> items</td>
                        <td><strong style="color:var(--cyan-500);">${d.totalUnitsInStock}</strong> units</td>
                        <td>$${parseFloat(d.minProductPrice).toFixed(2)}</td>
                        <td>$${parseFloat(d.maxProductPrice).toFixed(2)}</td>
                        <td>$${parseFloat(d.avgProductPrice).toFixed(2)}</td>
                        <td><strong style="color:var(--emerald-500); font-size:1.05rem;">$${parseFloat(d.totalInventoryValuation).toLocaleString('en-US', { minimumFractionDigits: 2 })}</strong></td>
                    </tr>
                `;
            } else if (queryId === 'q5') {
                const minCount = document.getElementById('q5-min-count').value || 2;
                const res = await API.getCategoryProductCount(minCount);
                thead.innerHTML = `<tr><th>Category ID</th><th>Category Name</th><th>Product Count (COUNT &ge; ${minCount})</th><th>Total Category Units</th><th>Category Valuation</th></tr>`;
                tbody.innerHTML = res.data.map(c => `
                    <tr>
                        <td>#${c.categoryId}</td>
                        <td><strong>${this.escapeHtml(c.categoryName)}</strong></td>
                        <td><strong style="color:var(--primary-500);">${c.productCount}</strong></td>
                        <td>${c.totalUnitsInCategory}</td>
                        <td>$${parseFloat(c.categoryValuation).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    </tr>
                `).join('');
            }
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="6" class="table-loader-cell" style="color:var(--rose-500);">Failed to execute query: ${err.message}</td></tr>`;
        }
    },

    // Product Modal Actions
    openProductModal(productId = null) {
        this.state.currentEditProductId = productId;
        const modal = document.getElementById('product-modal');
        const title = document.getElementById('product-modal-title');
        const form = document.getElementById('product-form');
        form.reset();

        const initialStockGroup = document.getElementById('form-initial-stock-group');

        if (productId) {
            title.textContent = 'Edit Product';
            initialStockGroup.style.display = 'none';
            const product = this.state.products.find(p => p.id === productId);
            if (product) {
                document.getElementById('form-product-id').value = product.id;
                document.getElementById('form-product-sku').value = product.sku;
                document.getElementById('form-product-name').value = product.name;
                document.getElementById('form-product-description').value = product.description || '';
                document.getElementById('form-product-price').value = product.price;
                document.getElementById('form-product-category').value = product.categoryId;
                document.getElementById('form-product-threshold').value = product.lowStockThreshold || 10;
            }
        } else {
            title.textContent = 'Create New Product';
            initialStockGroup.style.display = 'flex';
            document.getElementById('form-product-id').value = '';
        }

        modal.classList.add('active');
    },

    openEditProductModal(productId) {
        this.openProductModal(productId);
    },

    closeProductModal() {
        document.getElementById('product-modal').classList.remove('active');
    },

    async handleProductFormSubmit(e) {
        e.preventDefault();
        const productId = document.getElementById('form-product-id').value;
        const payload = {
            sku: document.getElementById('form-product-sku').value.trim(),
            name: document.getElementById('form-product-name').value.trim(),
            description: document.getElementById('form-product-description').value.trim(),
            price: parseFloat(document.getElementById('form-product-price').value),
            categoryId: parseInt(document.getElementById('form-product-category').value, 10),
            initialStock: parseInt(document.getElementById('form-product-stock').value, 10) || 0,
            lowStockThreshold: parseInt(document.getElementById('form-product-threshold').value, 10) || 10
        };

        try {
            if (productId) {
                await API.updateProduct(productId, payload);
                this.showToast('Product updated successfully!', 'success');
            } else {
                await API.createProduct(payload);
                this.showToast('Product created successfully!', 'success');
            }
            this.closeProductModal();
            this.loadAllData();
        } catch (err) {
            this.showToast(`Error: ${err.message}`, 'error');
        }
    },

    async confirmDeleteProduct(productId, productName) {
        if (confirm(`Are you sure you want to delete "${productName}"? This will also remove its inventory records.`)) {
            try {
                await API.deleteProduct(productId);
                this.showToast(`Product "${productName}" deleted.`, 'success');
                this.loadAllData();
            } catch (err) {
                this.showToast(`Error deleting product: ${err.message}`, 'error');
            }
        }
    },

    // Stock Operations (Stock-In / Stock-Out)
    openStockOpModal(productId, productName, productSku, currentStock, type) {
        this.state.selectedProductForStock = { productId, productName, productSku, currentStock };
        document.getElementById('stock-op-product-id').value = productId;
        document.getElementById('stock-op-type').value = type;
        document.getElementById('stock-op-pname').textContent = productName;
        document.getElementById('stock-op-psku').textContent = productSku;
        document.getElementById('stock-op-current-stock').textContent = currentStock;

        const modalTitle = document.getElementById('stock-op-modal-title');
        const qtyLabel = document.getElementById('stock-op-quantity-label');
        const submitBtn = document.getElementById('btn-submit-stock-op');

        if (type === 'STOCK_IN') {
            modalTitle.textContent = 'Stock-In (Replenish Inventory)';
            qtyLabel.innerHTML = 'Quantity to Add <span class="required">*</span>';
            submitBtn.textContent = 'Confirm Stock-In';
            submitBtn.className = 'btn btn-primary';
        } else {
            modalTitle.textContent = 'Stock-Out (Dispatch Inventory)';
            qtyLabel.innerHTML = 'Quantity to Dispatch <span class="required">* (Max ' + currentStock + ')</span>';
            submitBtn.textContent = 'Confirm Stock-Out';
            submitBtn.className = 'btn btn-danger-soft';
        }

        document.getElementById('stock-op-quantity').value = 1;
        document.getElementById('stock-op-notes').value = '';
        document.getElementById('stock-op-modal').classList.add('active');
    },

    closeStockOpModal() {
        document.getElementById('stock-op-modal').classList.remove('active');
    },

    async handleStockOpSubmit(e) {
        e.preventDefault();
        const productId = document.getElementById('stock-op-product-id').value;
        const type = document.getElementById('stock-op-type').value;
        const quantity = parseInt(document.getElementById('stock-op-quantity').value, 10);
        const notes = document.getElementById('stock-op-notes').value.trim();

        try {
            if (type === 'STOCK_IN') {
                await API.stockIn(productId, { quantity, notes });
                this.showToast(`Successfully added ${quantity} units to stock.`, 'success');
            } else {
                await API.stockOut(productId, { quantity, notes });
                this.showToast(`Successfully dispatched ${quantity} units from stock.`, 'success');
            }
            this.closeStockOpModal();
            this.loadAllData();
        } catch (err) {
            this.showToast(`Operation Failed: ${err.message}`, 'error');
        }
    },

    // Toast Notifications
    showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span class="toast-message">${this.escapeHtml(message)}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
};
