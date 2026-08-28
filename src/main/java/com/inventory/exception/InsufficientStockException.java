package com.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    private final Long productId;
    private final Integer availableStock;
    private final Integer requestedQuantity;

    public InsufficientStockException(String message) {
        super(message);
        this.productId = null;
        this.availableStock = null;
        this.requestedQuantity = null;
    }

    public InsufficientStockException(Long productId, Integer availableStock, Integer requestedQuantity) {
        super(String.format("Cannot perform stock-out of %d units for product ID %d. Available stock is only %d units.",
                requestedQuantity, productId, availableStock));
        this.productId = productId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }
}
