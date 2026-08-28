package com.inventory.dto;

import java.math.BigDecimal;

public class CategoryStockDto {
    private Long categoryId;
    private String categoryName;
    private Long totalProducts;
    private Long totalStockQuantity;
    private Double avgStockPerProduct;
    private BigDecimal categoryInventoryValuation;

    public CategoryStockDto() {
    }

    public CategoryStockDto(Long categoryId, String categoryName, Long totalProducts,
                            Long totalStockQuantity, Double avgStockPerProduct, BigDecimal categoryInventoryValuation) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalProducts = totalProducts;
        this.totalStockQuantity = totalStockQuantity;
        this.avgStockPerProduct = avgStockPerProduct;
        this.categoryInventoryValuation = categoryInventoryValuation;
    }

    public CategoryStockDto(Long categoryId, String categoryName, Number totalProducts,
                            Number totalStockQuantity, Number avgStockPerProduct, Number categoryInventoryValuation) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalProducts = totalProducts != null ? totalProducts.longValue() : 0L;
        this.totalStockQuantity = totalStockQuantity != null ? totalStockQuantity.longValue() : 0L;
        this.avgStockPerProduct = avgStockPerProduct != null ? avgStockPerProduct.doubleValue() : 0.0;
        this.categoryInventoryValuation = categoryInventoryValuation != null ? BigDecimal.valueOf(categoryInventoryValuation.doubleValue()) : BigDecimal.ZERO;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public Long getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public void setTotalStockQuantity(Long totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }

    public Double getAvgStockPerProduct() {
        return avgStockPerProduct;
    }

    public void setAvgStockPerProduct(Double avgStockPerProduct) {
        this.avgStockPerProduct = avgStockPerProduct;
    }

    public BigDecimal getCategoryInventoryValuation() {
        return categoryInventoryValuation;
    }

    public void setCategoryInventoryValuation(BigDecimal categoryInventoryValuation) {
        this.categoryInventoryValuation = categoryInventoryValuation;
    }
}
