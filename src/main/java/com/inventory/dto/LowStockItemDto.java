package com.inventory.dto;

import java.math.BigDecimal;

public class LowStockItemDto {
    private Long productId;
    private String sku;
    private String productName;
    private String categoryName;
    private BigDecimal price;
    private Integer currentStock;
    private Integer lowStockThreshold;
    private Integer deficit;
    private String stockStatus;

    public LowStockItemDto() {
    }

    public LowStockItemDto(Long productId, String sku, String productName, String categoryName,
                           BigDecimal price, Integer currentStock, Integer lowStockThreshold) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.categoryName = categoryName;
        this.price = price;
        this.currentStock = currentStock;
        this.lowStockThreshold = lowStockThreshold;
        this.deficit = lowStockThreshold != null && currentStock != null ? Math.max(0, lowStockThreshold - currentStock) : 0;
        this.stockStatus = (currentStock != null && currentStock == 0) ? "OUT_OF_STOCK" : "LOW_STOCK";
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getDeficit() {
        return deficit;
    }

    public void setDeficit(Integer deficit) {
        this.deficit = deficit;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }
}
