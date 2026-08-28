package com.inventory.dto;

import java.math.BigDecimal;

public class InventorySummaryDto {
    private Long totalDistinctProducts;
    private Long totalUnitsInStock;
    private Long lowStockProductCount;
    private Long outOfStockProductCount;
    private BigDecimal minProductPrice;
    private BigDecimal maxProductPrice;
    private BigDecimal avgProductPrice;
    private BigDecimal totalInventoryValuation;

    public InventorySummaryDto() {
    }

    public InventorySummaryDto(Long totalDistinctProducts, Long totalUnitsInStock, Long lowStockProductCount,
                               Long outOfStockProductCount, BigDecimal minProductPrice, BigDecimal maxProductPrice,
                               BigDecimal avgProductPrice, BigDecimal totalInventoryValuation) {
        this.totalDistinctProducts = totalDistinctProducts;
        this.totalUnitsInStock = totalUnitsInStock;
        this.lowStockProductCount = lowStockProductCount;
        this.outOfStockProductCount = outOfStockProductCount;
        this.minProductPrice = minProductPrice;
        this.maxProductPrice = maxProductPrice;
        this.avgProductPrice = avgProductPrice;
        this.totalInventoryValuation = totalInventoryValuation;
    }

    public Long getTotalDistinctProducts() {
        return totalDistinctProducts;
    }

    public void setTotalDistinctProducts(Long totalDistinctProducts) {
        this.totalDistinctProducts = totalDistinctProducts;
    }

    public Long getTotalUnitsInStock() {
        return totalUnitsInStock;
    }

    public void setTotalUnitsInStock(Long totalUnitsInStock) {
        this.totalUnitsInStock = totalUnitsInStock;
    }

    public Long getLowStockProductCount() {
        return lowStockProductCount;
    }

    public void setLowStockProductCount(Long lowStockProductCount) {
        this.lowStockProductCount = lowStockProductCount;
    }

    public Long getOutOfStockProductCount() {
        return outOfStockProductCount;
    }

    public void setOutOfStockProductCount(Long outOfStockProductCount) {
        this.outOfStockProductCount = outOfStockProductCount;
    }

    public BigDecimal getMinProductPrice() {
        return minProductPrice;
    }

    public void setMinProductPrice(BigDecimal minProductPrice) {
        this.minProductPrice = minProductPrice;
    }

    public BigDecimal getMaxProductPrice() {
        return maxProductPrice;
    }

    public void setMaxProductPrice(BigDecimal maxProductPrice) {
        this.maxProductPrice = maxProductPrice;
    }

    public BigDecimal getAvgProductPrice() {
        return avgProductPrice;
    }

    public void setAvgProductPrice(BigDecimal avgProductPrice) {
        this.avgProductPrice = avgProductPrice;
    }

    public BigDecimal getTotalInventoryValuation() {
        return totalInventoryValuation;
    }

    public void setTotalInventoryValuation(BigDecimal totalInventoryValuation) {
        this.totalInventoryValuation = totalInventoryValuation;
    }
}
