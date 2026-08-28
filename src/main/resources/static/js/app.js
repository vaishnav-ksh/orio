/**
 * ORIO Product Inventory Management - Application Controller
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
        currentEditProductId: null,
        selectedProductForStock: null
    },

    init() {
        this.bindEvents();
        this.loadCategories();
        this.loadAllData();
    },

    bindEvents() {
        // Tab Navigation
        document.querySelectorAll('.nav-tab').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const targetTab = tab.getAttribute('data-tab');
                this.switchTab(targetTab);
            });
        });

        // Search & Filter
        const searchInput = document.getElementById('product-search-input');
        let debounceTimeout = null;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimeout);
            debounceTimeout = setTimeout(() => {
                this.loadProducts();
            }, 300);
        });

        document.getElementById('product-category-filter').addEventListener('change', () => {
            this.loadProducts();
        });

        // Modals open/close
        document.getElementById('open-create-product-btn').addEventListener('click', () => {
            this.openProductModal();
        });

        document.getElementById('close-product-modal').addEventListener('click', () => this.closeProductModal());
        document.getElementById('cancel-product-modal-btn').addEventListener('click', () => this.closeProductModal());

        document.getElementById('close-stock-in-modal').addEventListener('click', () => this.closeStockInModal());
        document.getElementById('cancel-stock-in-btn').addEventListener('click', () => this.closeStockInModal());

        document.getElementById('close-stock-out-modal').addEventListener('click', () => this.closeStockOutModal());
        document.getElementById('cancel-stock-out-btn').addEventListener('click', () => this.closeStockOutModal());

        document.getElementById('close-threshold-modal').addEventListener('click', () => this.closeThresholdModal());
        document.getElementById('cancel-threshold-btn').addEventListener('click', () => this.closeThresholdModal());

        // Form Submissions
        document.getElementById('product-form').addEventListener('submit', (e) => this.handleProductFormSubmit(e));
        document.getElementById('stock-in-form').addEventListener('submit', (e) => this.handleStockInSubmit(e));
        document.getElementById('stock-out-form').addEventListener('submit', (e) => this.handleStockOutSubmit(e));
        document.getElementById('threshold-form').addEventListener('submit', (e) => this.handleThresholdSubmit(e));

        // Real-time stock-out input validation hint
        const stockOutQtyInput = document.getElementById('stock-out-qty');
        stockOutQtyInput.addEventListener('input', () => {
            if (this.state.selectedProductForStock) {
                const available = this.state.selectedProductForStock.currentStock || 0;
                const requested = parseInt(stockOutQtyInput.value, 10) || 0;
                const hint = document.getElementById('stock-out-error-hint');
                if (requested > available) {
                    hint.style.display = 'block';
                    hint.textContent = `Exceeds available stock (${available})!`;
                } else {
                    hint.style.display = 'none';
                }
            }
        });

        // Low stock threshold filter
        document.getElementById('apply-custom-threshold-btn').addEventListener('click', () => {
            const threshold = document.getElementById('custom-low-stock-threshold').value;
            this.loadLowStockItems(threshold);
        });

        // Refreshes
        document.getElementById('refresh-inventory-btn').addEventListener('click', () => this.loadInventory());
        document.getElementById('refresh-transactions-btn').addEventListener('click', () => this.loadTransactions());

        // SQL Analytics Query Execution Buttons
        document.getElementById('run-query-1-btn').addEventListener('click', () => this.runQuery1());
        document.getElementById('run-query-3-btn').addEventListener('click', () => this.runQuery3());
        document.getElementById('run-query-5-btn').addEventListener('click', () => {
            const count = document.getElementById('min-products-count-input').value || 2;
            this.runQuery5(count);
        });
    },

    switchTab(tabId) {
        document.querySelectorAll('.nav-tab').forEach(t => {
            t.classList.toggle('active', t.getAttribute('data-tab') === tabId);
        });
        document.querySelectorAll('.tab-content').forEach(c => {
            c.classList.toggle('active', c.id === tabId);
        });
        this.state.activeTab = tabId;

        // Auto load relevant tab data
        if (tabId === 'inventory-tab') this.loadInventory();
        if (tabId === 'low-stock-tab') this.loadLowStockItems();
        if (tabId === 'transactions-tab') this.loadTransactions();
        if (tabId === 'analytics-tab') {
            this.runQuery1();
            this.runQuery3();
            this.runQuery5(document.getElementById('min-products-count-input').value || 2);
        }
    },

    async loadAllData() {
        await Promise.all([
            this.loadKPIs(),
            this.loadProducts(),
            this.loadInventory(),
            this.loadLowStockItems()
        ]);
    },

    async loadKPIs() {
        try {
            const res = await Api.getSummary();
            if (res.success && res.data) {
                const s = res.data;
                document.getElementById('kpi-total-products').textContent = s.totalDistinctProducts.toLocaleString();
                document.getElementById('kpi-total-units').textContent = s.totalUnitsInStock.toLocaleString();
                document.getElementById('kpi-low-stock').textContent = (s.lowStockProductCount + s.outOfStockProductCount).toLocaleString();
                document.getElementById('kpi-total-valuation').textContent = this.formatCurrency(s.totalInventoryValuation);
                document.getElementById('low-stock-badge-count').textContent = s.lowStockProductCount + s.outOfStockProductCount;
            }
        } catch (err) {
            console.error('Failed to load KPIs:', err);
        }
    },

    async loadCategories() {
        try {
            const res = await Api.getCategories();
            if (res.success) {
                this.state.categories = res.data;
                
                // Populate filters & forms
                const filterSelect = document.getElementById('product-category-filter');
                const formSelect = document.getElementById('form-category');
                
                filterSelect.innerHTML = '<option value="">All Categories</option>';
                formSelect.innerHTML = '<option value="">-- Select Category --</option>';

                this.state.categories.forEach(cat => {
                    filterSelect.innerHTML += `<option value="${cat.id}">${this.escapeHtml(cat.name)}</option>`;
                    formSelect.innerHTML += `<option value="${cat.id}">${this.escapeHtml(cat.name)}</option>`;
                });
            }
        } catch (err) {
            console.error('Failed to load categories:', err);
        }
    },

    async loadProducts() {
        const search = document.getElementById('product-search-input').value;
        const categoryId = document.getElementById('product-category-filter').value;
        
        try {
            const res = await Api.getProducts({ search, categoryId });
            if (res.success) {
                this.state.products = res.data;
                this.renderProductsTable();
            }
        } catch (err) {
            this.showToast('Failed to load products: ' + err.message, 'error');
        }
    },

    renderProductsTable() {
        const tbody = document.getElementById('products-table-body');
        if (!this.state.products || this.state.products.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No products found.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.state.products.map(p => `
            <tr>
                <td><span class="sku-tag">${this.escapeHtml(p.sku)}</span></td>
                <td>
                    <div style="font-weight: 600;">${this.escapeHtml(p.name)}</div>
                    <div style="font-size: 0.78rem; color: var(--text-muted);">${this.escapeHtml(p.description || '')}</div>
                </td>
                <td><span class="badge badge-category">${this.escapeHtml(p.categoryName || 'General')}</span></td>
                <td style="font-weight: 600;">${this.formatCurrency(p.price)}</td>
                <td style="font-weight: 700; font-size: 0.95rem;">${p.stockQuantity}</td>
                <td>${this.renderStatusBadge(p.stockStatus)}</td>
                <td>${this.formatCurrency(p.totalValuation)}</td>
                <td style="text-align: right;">
                    <div class="action-btns" style="justify-content: flex-end;">
                        <button class="btn btn-secondary btn-sm" onclick="App.openStockInModal(${p.id})">
                            <span style="color: #34d399;">+ Stock In</span>
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="App.openStockOutModal(${p.id})">
                            <span style="color: #f87171;">- Stock Out</span>
                        </button>
                        <button class="btn btn-secondary btn-sm btn-icon" title="Edit Product" onclick="App.openEditProductModal(${p.id})">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/></svg>
                        </button>
                        <button class="btn btn-danger btn-sm btn-icon" title="Delete Product" onclick="App.deleteProduct(${p.id}, '${this.escapeHtml(p.name)}')">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    async loadInventory() {
        try {
            const res = await Api.getInventory();
            if (res.success) {
                this.state.inventory = res.data;
                this.renderInventoryTable();
            }
        } catch (err) {
            this.showToast('Failed to load inventory: ' + err.message, 'error');
        }
    },

    renderInventoryTable() {
        const tbody = document.getElementById('inventory-table-body');
        if (!this.state.inventory || this.state.inventory.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No inventory records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.state.inventory.map(inv => `
            <tr>
                <td>
                    <div style="font-weight: 600;">${this.escapeHtml(inv.productName)}</div>
                    <span class="sku-tag">${this.escapeHtml(inv.productSku)}</span>
                </td>
                <td><span class="badge badge-category">${this.escapeHtml(inv.categoryName)}</span></td>
                <td>
                    <span style="font-size: 1.1rem; font-weight: 700; color: ${inv.currentStock === 0 ? '#ef4444' : (inv.currentStock <= inv.lowStockThreshold ? '#f59e0b' : '#10b981')}">
                        ${inv.currentStock}
                    </span> units
                </td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="App.openThresholdModal(${inv.productId}, '${this.escapeHtml(inv.productName)}', ${inv.lowStockThreshold})">
                        ${inv.lowStockThreshold} units <span style="font-size: 0.7rem; color: var(--text-muted);">&bull; Edit</span>
                    </button>
                </td>
                <td>${this.renderStatusBadge(inv.stockStatus)}</td>
                <td style="font-size: 0.8rem; color: var(--text-muted);">
                    ${inv.lastRestockedAt ? this.formatDate(inv.lastRestockedAt) : 'Never'}
                </td>
                <td style="text-align: right;">
                    <div class="action-btns" style="justify-content: flex-end;">
                        <button class="btn btn-success btn-sm" onclick="App.openStockInModal(${inv.productId})">+ Stock In</button>
                        <button class="btn btn-danger btn-sm" onclick="App.openStockOutModal(${inv.productId})">- Stock Out</button>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    async loadLowStockItems(threshold = null) {
        try {
            const res = await Api.getLowStock(threshold);
            if (res.success) {
                this.state.lowStockItems = res.data;
                this.renderLowStockTable();
                document.getElementById('low-stock-badge-count').textContent = this.state.lowStockItems.length;
            }
        } catch (err) {
            this.showToast('Failed to load low stock items: ' + err.message, 'error');
        }
    },

    renderLowStockTable() {
        const tbody = document.getElementById('low-stock-table-body');
        if (!this.state.lowStockItems || this.state.lowStockItems.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="empty-state">🎉 All products are well stocked above their threshold!</td></tr>`;
            return;
        }

        tbody.innerHTML = this.state.lowStockItems.map(item => `
            <tr>
                <td><span class="sku-tag">${this.escapeHtml(item.sku)}</span></td>
                <td style="font-weight: 600;">${this.escapeHtml(item.productName)}</td>
                <td><span class="badge badge-category">${this.escapeHtml(item.categoryName)}</span></td>
                <td>
                    <strong style="color: ${item.currentStock === 0 ? '#ef4444' : '#f59e0b'}; font-size: 1.05rem;">
                        ${item.currentStock}
                    </strong>
                </td>
                <td>${item.lowStockThreshold}</td>
                <td><span style="color: #ef4444; font-weight: 600;">-${item.deficit}</span></td>
                <td>${this.renderStatusBadge(item.stockStatus)}</td>
                <td style="text-align: right;">
                    <button class="btn btn-success btn-sm" onclick="App.openStockInModal(${item.productId})">
                        + Restock Now
                    </button>
                </td>
            </tr>
        `).join('');
    },

    async loadTransactions() {
        try {
            const res = await Api.getTransactions();
            if (res.success) {
                this.state.transactions = res.data;
                this.renderTransactionsTable();
            }
        } catch (err) {
            this.showToast('Failed to load transactions: ' + err.message, 'error');
        }
    },

    renderTransactionsTable() {
        const tbody = document.getElementById('transactions-table-body');
        if (!this.state.transactions || this.state.transactions.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No transaction logs recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = this.state.transactions.map(t => {
            const isStockIn = t.transactionType === 'STOCK_IN';
            return `
                <tr>
                    <td style="font-size: 0.8rem; color: var(--text-muted);">${this.formatDate(t.transactionTime)}</td>
                    <td>
                        <div style="font-weight: 600;">${this.escapeHtml(t.productName)}</div>
                        <span class="sku-tag">${this.escapeHtml(t.productSku)}</span>
                    </td>
                    <td>
                        <span class="badge ${isStockIn ? 'badge-in-stock' : 'badge-out-of-stock'}">
                            ${isStockIn ? 'STOCK_IN' : 'STOCK_OUT'}
                        </span>
                    </td>
                    <td style="font-weight: 700; color: ${isStockIn ? '#34d399' : '#f87171'};">
                        ${isStockIn ? '+' : '-'}${t.quantity}
                    </td>
                    <td style="font-family: monospace;">${t.previousStock} &rarr; <strong>${t.newStock}</strong></td>
                    <td style="color: var(--text-secondary); font-size: 0.85rem;">${this.escapeHtml(t.notes || '—')}</td>
                </tr>
            `;
        }).join('');
    },

    // SQL Analytical Queries
    async runQuery1() {
        const container = document.getElementById('query-1-results');
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;">Executing Query 1...</div>';
        try {
            const res = await Api.getAvailableStockProducts();
            if (res.success && res.data) {
                container.innerHTML = `
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr><th>Product</th><th>Category</th><th>Price</th><th>Available Stock</th><th>Valuation</th></tr>
                            </thead>
                            <tbody>
                                ${res.data.map(p => `
                                    <tr>
                                        <td><strong>${this.escapeHtml(p.name)}</strong> (${p.sku})</td>
                                        <td>${this.escapeHtml(p.categoryName)}</td>
                                        <td>${this.formatCurrency(p.price)}</td>
                                        <td><strong style="color: #34d399;">${p.stockQuantity}</strong></td>
                                        <td>${this.formatCurrency(p.totalValuation)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            }
        } catch (err) {
            container.innerHTML = `<div style="color: var(--danger);">Error executing query: ${err.message}</div>`;
        }
    },

    async runQuery3() {
        const container = document.getElementById('query-3-results');
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;">Executing Query 3...</div>';
        try {
            const res = await Api.getStockByCategory();
            if (res.success && res.data) {
                container.innerHTML = `
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr><th>Category</th><th>Total Products</th><th>Total Stock Quantity</th><th>Avg Stock / Product</th><th>Category Inventory Valuation</th></tr>
                            </thead>
                            <tbody>
                                ${res.data.map(c => `
                                    <tr>
                                        <td><strong>${this.escapeHtml(c.categoryName)}</strong></td>
                                        <td>${c.totalProducts}</td>
                                        <td><strong style="color: #38bdf8;">${c.totalStockQuantity} units</strong></td>
                                        <td>${c.avgStockPerProduct.toFixed(1)}</td>
                                        <td>${this.formatCurrency(c.categoryInventoryValuation)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            }
        } catch (err) {
            container.innerHTML = `<div style="color: var(--danger);">Error executing query: ${err.message}</div>`;
        }
    },

    async runQuery5(minCount = 2) {
        const container = document.getElementById('query-5-results');
        container.innerHTML = `<div style="color: var(--text-muted); font-size: 0.85rem;">Executing Query 5 with minCount = ${minCount}...</div>`;
        try {
            const res = await Api.getCategoriesByMinProducts(minCount);
            if (res.success && res.data) {
                if (res.data.length === 0) {
                    container.innerHTML = `<div style="color: var(--text-muted); padding: 0.5rem 0;">No categories have &ge; ${minCount} products.</div>`;
                    return;
                }
                container.innerHTML = `
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr><th>Category Name</th><th>Product Count</th><th>Total Units</th><th>Category Valuation</th></tr>
                            </thead>
                            <tbody>
                                ${res.data.map(row => `
                                    <tr>
                                        <td><strong>${this.escapeHtml(row.categoryName)}</strong></td>
                                        <td><span class="badge badge-category">${row.productCount} products</span></td>
                                        <td>${row.totalUnits} units</td>
                                        <td>${this.formatCurrency(row.categoryValuation)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            }
        } catch (err) {
            container.innerHTML = `<div style="color: var(--danger);">Error executing query: ${err.message}</div>`;
        }
    },

    // Modal Operations
    openProductModal(product = null) {
        const modal = document.getElementById('product-modal');
        const form = document.getElementById('product-form');
        form.reset();

        if (product) {
            document.getElementById('product-modal-title').textContent = 'Edit Product';
            document.getElementById('form-product-id').value = product.id;
            document.getElementById('form-sku').value = product.sku;
            document.getElementById('form-name').value = product.name;
            document.getElementById('form-description').value = product.description || '';
            document.getElementById('form-price').value = product.price;
            document.getElementById('form-category').value = product.categoryId;
            document.getElementById('form-initial-stock-group').style.display = 'none';
            document.getElementById('form-low-threshold').value = product.lowStockThreshold || 10;
        } else {
            document.getElementById('product-modal-title').textContent = 'Create New Product';
            document.getElementById('form-product-id').value = '';
            document.getElementById('form-initial-stock-group').style.display = 'block';
            document.getElementById('form-initial-stock').value = '0';
            document.getElementById('form-low-threshold').value = '10';
        }

        modal.classList.add('active');
    },

    async openEditProductModal(id) {
        try {
            const res = await Api.getProductById(id);
            if (res.success) {
                this.openProductModal(res.data);
            }
        } catch (err) {
            this.showToast('Failed to load product: ' + err.message, 'error');
        }
    },

    closeProductModal() {
        document.getElementById('product-modal').classList.remove('active');
    },

    async handleProductFormSubmit(e) {
        e.preventDefault();
        const id = document.getElementById('form-product-id').value;
        const price = parseFloat(document.getElementById('form-price').value);

        if (isNaN(price) || price < 0) {
            this.showToast('Product price cannot be negative', 'error');
            return;
        }

        const payload = {
            sku: document.getElementById('form-sku').value.trim(),
            name: document.getElementById('form-name').value.trim(),
            description: document.getElementById('form-description').value.trim(),
            price: price,
            categoryId: parseInt(document.getElementById('form-category').value, 10),
            lowStockThreshold: parseInt(document.getElementById('form-low-threshold').value, 10)
        };

        if (!id) {
            payload.initialStock = parseInt(document.getElementById('form-initial-stock').value, 10) || 0;
            if (payload.initialStock < 0) {
                this.showToast('Initial stock cannot be negative', 'error');
                return;
            }
        }

        try {
            if (id) {
                await Api.updateProduct(id, payload);
                this.showToast('Product updated successfully!', 'success');
            } else {
                await Api.createProduct(payload);
                this.showToast('Product created successfully!', 'success');
            }
            this.closeProductModal();
            this.loadAllData();
        } catch (err) {
            const msg = err.data?.validationErrors ? Object.values(err.data.validationErrors).join(', ') : err.message;
            this.showToast(msg, 'error');
        }
    },

    async deleteProduct(id, name) {
        if (!confirm(`Are you sure you want to delete product "${name}"? This action cannot be undone.`)) {
            return;
        }
        try {
            await Api.deleteProduct(id);
            this.showToast(`Product "${name}" deleted successfully`, 'success');
            this.loadAllData();
        } catch (err) {
            this.showToast('Failed to delete product: ' + err.message, 'error');
        }
    },

    // Stock In Modal
    async openStockInModal(productId) {
        try {
            const res = await Api.getProductById(productId);
            if (res.success) {
                const product = res.data;
                this.state.selectedProductForStock = product;
                document.getElementById('stock-in-product-id').value = product.id;
                document.getElementById('stock-in-product-name').textContent = product.name;
                document.getElementById('stock-in-product-sku').textContent = product.sku;
                document.getElementById('stock-in-current-stock').textContent = product.stockQuantity;
                document.getElementById('stock-in-qty').value = '';
                document.getElementById('stock-in-notes').value = '';
                document.getElementById('stock-in-modal').classList.add('active');
            }
        } catch (err) {
            this.showToast('Error loading product details: ' + err.message, 'error');
        }
    },

    closeStockInModal() {
        document.getElementById('stock-in-modal').classList.remove('active');
        this.state.selectedProductForStock = null;
    },

    async handleStockInSubmit(e) {
        e.preventDefault();
        const productId = document.getElementById('stock-in-product-id').value;
        const qty = parseInt(document.getElementById('stock-in-qty').value, 10);
        const notes = document.getElementById('stock-in-notes').value;

        if (isNaN(qty) || qty <= 0) {
            this.showToast('Quantity must be greater than 0', 'error');
            return;
        }

        try {
            const res = await Api.stockIn(productId, qty, notes);
            if (res.success) {
                this.showToast(`Stock-in confirmed: +${qty} units added! New stock: ${res.data.currentStock}`, 'success');
                this.closeStockInModal();
                this.loadAllData();
            }
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    // Stock Out Modal
    async openStockOutModal(productId) {
        try {
            const res = await Api.getProductById(productId);
            if (res.success) {
                const product = res.data;
                this.state.selectedProductForStock = product;
                document.getElementById('stock-out-product-id').value = product.id;
                document.getElementById('stock-out-product-name').textContent = product.name;
                document.getElementById('stock-out-product-sku').textContent = product.sku;
                document.getElementById('stock-out-current-stock').textContent = product.stockQuantity;
                document.getElementById('stock-out-qty').value = '';
                document.getElementById('stock-out-qty').max = product.stockQuantity;
                document.getElementById('stock-out-notes').value = '';
                document.getElementById('stock-out-error-hint').style.display = 'none';
                document.getElementById('stock-out-modal').classList.add('active');
            }
        } catch (err) {
            this.showToast('Error loading product details: ' + err.message, 'error');
        }
    },

    closeStockOutModal() {
        document.getElementById('stock-out-modal').classList.remove('active');
        this.state.selectedProductForStock = null;
    },

    async handleStockOutSubmit(e) {
        e.preventDefault();
        const productId = document.getElementById('stock-out-product-id').value;
        const qty = parseInt(document.getElementById('stock-out-qty').value, 10);
        const notes = document.getElementById('stock-out-notes').value;

        if (isNaN(qty) || qty <= 0) {
            this.showToast('Quantity must be greater than 0', 'error');
            return;
        }

        try {
            const res = await Api.stockOut(productId, qty, notes);
            if (res.success) {
                this.showToast(`Stock-out confirmed: -${qty} units dispatched. Remaining stock: ${res.data.currentStock}`, 'success');
                this.closeStockOutModal();
                this.loadAllData();
            }
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    // Threshold Modal
    openThresholdModal(productId, productName, currentThreshold) {
        document.getElementById('threshold-product-id').value = productId;
        document.getElementById('threshold-product-name').textContent = productName;
        document.getElementById('threshold-value').value = currentThreshold;
        document.getElementById('threshold-modal').classList.add('active');
    },

    closeThresholdModal() {
        document.getElementById('threshold-modal').classList.remove('active');
    },

    async handleThresholdSubmit(e) {
        e.preventDefault();
        const productId = document.getElementById('threshold-product-id').value;
        const threshold = parseInt(document.getElementById('threshold-value').value, 10);

        if (isNaN(threshold) || threshold < 0) {
            this.showToast('Low stock threshold cannot be negative', 'error');
            return;
        }

        try {
            await Api.updateThreshold(productId, threshold);
            this.showToast('Low-stock threshold updated!', 'success');
            this.closeThresholdModal();
            this.loadAllData();
        } catch (err) {
            this.showToast(err.message, 'error');
        }
    },

    // Toast Notifications
    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        let iconSvg = '';
        if (type === 'success') {
            iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>`;
        } else if (type === 'error') {
            iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`;
        } else {
            iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0ea5e9" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`;
        }

        toast.innerHTML = `${iconSvg} <span>${this.escapeHtml(message)}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    },

    // Utilities
    renderStatusBadge(status) {
        if (status === 'IN_STOCK') {
            return `<span class="badge badge-in-stock">&bull; In Stock</span>`;
        } else if (status === 'LOW_STOCK') {
            return `<span class="badge badge-low-stock">&bull; Low Stock</span>`;
        } else {
            return `<span class="badge badge-out-of-stock">&bull; Out of Stock</span>`;
        }
    },

    formatCurrency(val) {
        if (val === null || val === undefined) return '$0.00';
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
    },

    formatDate(dateStr) {
        if (!dateStr) return '—';
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }
};

window.App = App;
