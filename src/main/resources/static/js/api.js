/**
 * ORIO Product Inventory Management - Asynchronous API Client
 */

const API_BASE = '/api';

const Api = {
    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        };

        const config = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };

        try {
            const response = await fetch(url, config);
            const data = await response.json();

            if (!response.ok) {
                const errorMessage = data.message || data.error || `HTTP Error ${response.status}`;
                const error = new Error(errorMessage);
                error.data = data;
                error.status = response.status;
                throw error;
            }

            return data;
        } catch (err) {
            console.error(`API Error on [${config.method || 'GET'} ${url}]:`, err);
            throw err;
        }
    },

    // Products API
    getProducts(params = {}) {
        const query = new URLSearchParams();
        if (params.search) query.append('search', params.search);
        if (params.categoryId) query.append('categoryId', params.categoryId);
        if (params.inStockOnly) query.append('inStockOnly', 'true');
        
        const qs = query.toString() ? `?${query.toString()}` : '';
        return this.request(`${API_BASE}/products${qs}`);
    },

    getProductById(id) {
        return this.request(`${API_BASE}/products/${id}`);
    },

    createProduct(productData) {
        return this.request(`${API_BASE}/products`, {
            method: 'POST',
            body: JSON.stringify(productData)
        });
    },

    updateProduct(id, productData) {
        return this.request(`${API_BASE}/products/${id}`, {
            method: 'PUT',
            body: JSON.stringify(productData)
        });
    },

    deleteProduct(id) {
        return this.request(`${API_BASE}/products/${id}`, {
            method: 'DELETE'
        });
    },

    // Categories API
    getCategories() {
        return this.request(`${API_BASE}/categories`);
    },

    createCategory(categoryData) {
        return this.request(`${API_BASE}/categories`, {
            method: 'POST',
            body: JSON.stringify(categoryData)
        });
    },

    // Inventory API
    getInventory() {
        return this.request(`${API_BASE}/inventory`);
    },

    getInventoryByProduct(productId) {
        return this.request(`${API_BASE}/inventory/${productId}`);
    },

    stockIn(productId, quantity, notes = '') {
        return this.request(`${API_BASE}/inventory/${productId}/stock-in`, {
            method: 'POST',
            body: JSON.stringify({ quantity: Number(quantity), notes })
        });
    },

    stockOut(productId, quantity, notes = '') {
        return this.request(`${API_BASE}/inventory/${productId}/stock-out`, {
            method: 'POST',
            body: JSON.stringify({ quantity: Number(quantity), notes })
        });
    },

    getLowStock(threshold = null) {
        const qs = threshold !== null && threshold !== '' ? `?threshold=${threshold}` : '';
        return this.request(`${API_BASE}/inventory/low-stock${qs}`);
    },

    updateThreshold(productId, threshold) {
        return this.request(`${API_BASE}/inventory/${productId}/threshold?threshold=${threshold}`, {
            method: 'PATCH'
        });
    },

    getTransactions() {
        return this.request(`${API_BASE}/inventory/transactions`);
    },

    // Analytics API (SQL Tasks)
    getSummary() {
        return this.request(`${API_BASE}/analytics/summary`);
    },

    getStockByCategory() {
        return this.request(`${API_BASE}/analytics/stock-by-category`);
    },

    getAvailableStockProducts() {
        return this.request(`${API_BASE}/analytics/available-stock`);
    },

    getLowStockProducts() {
        return this.request(`${API_BASE}/analytics/low-stock-products`);
    },

    getCategoriesByMinProducts(minCount = 2) {
        return this.request(`${API_BASE}/analytics/category-product-count?minCount=${minCount}`);
    }
};

window.Api = Api;
