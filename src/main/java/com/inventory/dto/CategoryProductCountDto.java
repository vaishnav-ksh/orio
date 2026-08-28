package com.inventory.dto;

import java.math.BigDecimal;

public class CategoryProductCountDto {
    private Long categoryId;
    private String categoryName;
    private Long productCount;
    private Long totalUnits;
    private BigDecimal categoryValuation;

    public CategoryProductCountDto() {
    }

    public CategoryProductCountDto(Long categoryId, String categoryName, Long productCount, Long totalUnits, BigDecimal categoryValuation) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.productCount = productCount;
        this.totalUnits = totalUnits;
        this.categoryValuation = categoryValuation;
    }

    public CategoryProductCountDto(Long categoryId, String categoryName, Number productCount, Number totalUnits, Number categoryValuation) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.productCount = productCount != null ? productCount.longValue() : 0L;
        this.totalUnits = totalUnits != null ? totalUnits.longValue() : 0L;
        this.categoryValuation = categoryValuation != null ? BigDecimal.valueOf(categoryValuation.doubleValue()) : BigDecimal.ZERO;
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

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public Long getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Long totalUnits) {
        this.totalUnits = totalUnits;
    }

    public BigDecimal getCategoryValuation() {
        return categoryValuation;
    }

    public void setCategoryValuation(BigDecimal categoryValuation) {
        this.categoryValuation = categoryValuation;
    }
}
