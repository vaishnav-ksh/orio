package com.inventory.service;

import com.inventory.dto.*;
import com.inventory.entity.Product;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductService productService;

    public AnalyticsService(ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            InventoryRepository inventoryRepository,
                            ProductService productService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.productService = productService;
    }

    /**
     * Query 1: Products with Available Stock (JOIN products & inventories WHERE quantity > 0)
     */
    public List<ProductResponseDto> getProductsWithAvailableStock() {
        return productRepository.findProductsWithAvailableStock().stream()
                .map(productService::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Query 2: Low-Stock Products (JOIN products & inventories WHERE quantity <= threshold)
     */
    public List<ProductResponseDto> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(productService::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Query 3: Stock by Category (JOIN categories, products, inventories with GROUP BY, SUM, COUNT, AVG, ORDER BY)
     */
    public List<CategoryStockDto> getStockByCategory() {
        return categoryRepository.getStockByCategory();
    }

    /**
     * Query 4: Total Inventory Value & Global Metrics (JOIN products & inventories with SUM(price * quantity))
     */
    public InventorySummaryDto getTotalInventorySummary() {
        Object[] raw = inventoryRepository.getInventoryOverallSummary();
        if (raw != null && raw.length > 0) {
            Object[] row = (raw[0] instanceof Object[]) ? (Object[]) raw[0] : raw;
            
            long totalProducts = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            long totalUnits = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long lowStock = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long outOfStock = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            BigDecimal minPrice = row[4] != null ? BigDecimal.valueOf(((Number) row[4]).doubleValue()) : BigDecimal.ZERO;
            BigDecimal maxPrice = row[5] != null ? BigDecimal.valueOf(((Number) row[5]).doubleValue()) : BigDecimal.ZERO;
            BigDecimal avgPrice = row[6] != null ? BigDecimal.valueOf(((Number) row[6]).doubleValue()) : BigDecimal.ZERO;
            BigDecimal totalValuation = row[7] != null ? BigDecimal.valueOf(((Number) row[7]).doubleValue()) : BigDecimal.ZERO;

            return new InventorySummaryDto(
                    totalProducts,
                    totalUnits,
                    lowStock,
                    outOfStock,
                    minPrice,
                    maxPrice,
                    avgPrice,
                    totalValuation
            );
        }

        return new InventorySummaryDto(0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Query 5: Categories containing more than a specified number of products (GROUP BY & HAVING COUNT(p.id) >= minCount)
     */
    public List<CategoryProductCountDto> getCategoriesByMinProducts(long minCount) {
        return categoryRepository.findCategoriesWithProductCountGreaterThanEqual(minCount);
    }
}
