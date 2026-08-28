package com.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductRequestDto {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Product price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock = 0;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold = 10;

    public ProductRequestDto() {
    }

    public ProductRequestDto(String sku, String name, String description, BigDecimal price, Long categoryId, Integer initialStock, Integer lowStockThreshold) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.initialStock = initialStock != null ? initialStock : 0;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 10;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(Integer initialStock) {
        this.initialStock = initialStock;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }
}
