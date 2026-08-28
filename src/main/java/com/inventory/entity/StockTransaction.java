package com.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Transaction quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Previous stock is required")
    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @NotNull(message = "New stock is required")
    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    @Column(length = 255)
    private String notes;

    @Column(name = "transaction_time", updatable = false)
    private LocalDateTime transactionTime;

    public StockTransaction() {
    }

    public StockTransaction(Product product, TransactionType transactionType, Integer quantity,
                            Integer previousStock, Integer newStock, String notes) {
        this.product = product;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.transactionTime = LocalDateTime.now();
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }
}
