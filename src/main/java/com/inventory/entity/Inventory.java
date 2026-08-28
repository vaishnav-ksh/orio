package com.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @JsonIgnore
    private Product product;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity = 0;

    @NotNull(message = "Low stock threshold cannot be null")
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory() {
    }

    public Inventory(Product product, Integer quantity, Integer lowStockThreshold) {
        this.product = product;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return this.quantity != null && this.lowStockThreshold != null && this.quantity <= this.lowStockThreshold;
    }

    public boolean isOutOfStock() {
        return this.quantity != null && this.quantity == 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public LocalDateTime getLastRestockedAt() {
        return lastRestockedAt;
    }

    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
