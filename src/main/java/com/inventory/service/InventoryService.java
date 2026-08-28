package com.inventory.service;

import com.inventory.dto.InventoryResponseDto;
import com.inventory.dto.LowStockItemDto;
import com.inventory.dto.StockOperationRequest;
import com.inventory.dto.StockTransactionDto;
import com.inventory.entity.Inventory;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidOperationException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final int defaultLowStockThreshold;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository,
                            StockTransactionRepository stockTransactionRepository,
                            @Value("${app.inventory.default-low-stock-threshold:10}") int defaultLowStockThreshold) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.stockTransactionRepository = stockTransactionRepository;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryResponseDto getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + productId));
        return mapToResponseDto(inventory);
    }

    /**
     * Business Rule: Stock-In operation increments inventory and audits transaction.
     */
    public InventoryResponseDto stockIn(Long productId, StockOperationRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOperationException("Stock-in quantity must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory inv = new Inventory(product, 0, defaultLowStockThreshold);
                    product.setInventory(inv);
                    return inv;
                });

        int previousStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int incomingQuantity = request.getQuantity();
        int newStock = previousStock + incomingQuantity;

        inventory.setQuantity(newStock);
        inventory.setLastRestockedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);

        // Record Audit Transaction
        StockTransaction transaction = new StockTransaction(
                product,
                TransactionType.STOCK_IN,
                incomingQuantity,
                previousStock,
                newStock,
                request.getNotes() != null ? request.getNotes() : "Stock received"
        );
        stockTransactionRepository.save(transaction);

        return mapToResponseDto(saved);
    }

    /**
     * Business Rule: Stock-Out cannot exceed available stock; Stock cannot become negative.
     */
    public InventoryResponseDto stockOut(Long productId, StockOperationRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOperationException("Stock-out quantity must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + productId));

        int currentStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int requestedQuantity = request.getQuantity();

        // Business Rule Validation: Cannot exceed available stock
        if (requestedQuantity > currentStock) {
            throw new InsufficientStockException(productId, currentStock, requestedQuantity);
        }

        int newStock = currentStock - requestedQuantity;
        inventory.setQuantity(newStock);
        Inventory saved = inventoryRepository.save(inventory);

        // Record Audit Transaction
        StockTransaction transaction = new StockTransaction(
                product,
                TransactionType.STOCK_OUT,
                requestedQuantity,
                currentStock,
                newStock,
                request.getNotes() != null ? request.getNotes() : "Stock dispatched"
        );
        stockTransactionRepository.save(transaction);

        return mapToResponseDto(saved);
    }

    /**
     * Query 2 / Low-stock business requirement: Retrieve products below/at threshold
     */
    @Transactional(readOnly = true)
    public List<LowStockItemDto> getLowStockItems(Integer customThreshold) {
        List<Inventory> lowStockInventories;
        if (customThreshold != null) {
            lowStockInventories = inventoryRepository.findByQuantityLessThanEqual(customThreshold);
        } else {
            lowStockInventories = inventoryRepository.findAllLowStock();
        }

        return lowStockInventories.stream()
                .map(inv -> {
                    Product p = inv.getProduct();
                    String categoryName = p.getCategory() != null ? p.getCategory().getName() : "Uncategorized";
                    return new LowStockItemDto(
                            p.getId(),
                            p.getSku(),
                            p.getName(),
                            categoryName,
                            p.getPrice(),
                            inv.getQuantity(),
                            inv.getLowStockThreshold()
                    );
                })
                .collect(Collectors.toList());
    }

    public InventoryResponseDto updateThreshold(Long productId, int threshold) {
        if (threshold < 0) {
            throw new InvalidOperationException("Low stock threshold cannot be negative");
        }
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + productId));
        inventory.setLowStockThreshold(threshold);
        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StockTransactionDto> getRecentTransactions() {
        return stockTransactionRepository.findTop50ByOrderByTransactionTimeDesc().stream()
                .map(this::mapTransactionToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockTransactionDto> getTransactionsForProduct(Long productId) {
        return stockTransactionRepository.findByProductIdOrderByTransactionTimeDesc(productId).stream()
                .map(this::mapTransactionToDto)
                .collect(Collectors.toList());
    }

    private InventoryResponseDto mapToResponseDto(Inventory inventory) {
        Product p = inventory.getProduct();
        InventoryResponseDto dto = new InventoryResponseDto();
        dto.setInventoryId(inventory.getId());
        dto.setProductId(p.getId());
        dto.setProductSku(p.getSku());
        dto.setProductName(p.getName());
        dto.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized");
        dto.setProductPrice(p.getPrice());
        
        int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int threshold = inventory.getLowStockThreshold() != null ? inventory.getLowStockThreshold() : defaultLowStockThreshold;
        dto.setCurrentStock(qty);
        dto.setLowStockThreshold(threshold);
        dto.setLastRestockedAt(inventory.getLastRestockedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        dto.setTotalValuation(p.getPrice().multiply(BigDecimal.valueOf(qty)));

        if (qty == 0) {
            dto.setStockStatus("OUT_OF_STOCK");
        } else if (qty <= threshold) {
            dto.setStockStatus("LOW_STOCK");
        } else {
            dto.setStockStatus("IN_STOCK");
        }

        return dto;
    }

    private StockTransactionDto mapTransactionToDto(StockTransaction txn) {
        return new StockTransactionDto(
                txn.getId(),
                txn.getProduct().getId(),
                txn.getProduct().getSku(),
                txn.getProduct().getName(),
                txn.getTransactionType(),
                txn.getQuantity(),
                txn.getPreviousStock(),
                txn.getNewStock(),
                txn.getNotes(),
                txn.getTransactionTime()
        );
    }
}
