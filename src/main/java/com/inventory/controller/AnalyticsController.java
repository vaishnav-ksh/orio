package com.inventory.controller;

import com.inventory.dto.*;
import com.inventory.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Query 4: Total Inventory Value & High-Level KPI Summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventorySummaryDto>> getInventorySummary() {
        InventorySummaryDto summary = analyticsService.getTotalInventorySummary();
        return ResponseEntity.ok(ApiResponse.ok("Inventory summary metrics calculated", summary));
    }

    /**
     * Query 3: Stock by Category (JOIN, GROUP BY, SUM, COUNT, AVG, ORDER BY)
     */
    @GetMapping("/stock-by-category")
    public ResponseEntity<ApiResponse<List<CategoryStockDto>>> getStockByCategory() {
        List<CategoryStockDto> list = analyticsService.getStockByCategory();
        return ResponseEntity.ok(ApiResponse.ok("Stock aggregated by category", list));
    }

    /**
     * Query 1: Products with Available Stock (quantity > 0)
     */
    @GetMapping("/available-stock")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsWithAvailableStock() {
        List<ProductResponseDto> list = analyticsService.getProductsWithAvailableStock();
        return ResponseEntity.ok(ApiResponse.ok("Products with available stock fetched", list));
    }

    /**
     * Query 2: Low-Stock Products
     */
    @GetMapping("/low-stock-products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getLowStockProducts() {
        List<ProductResponseDto> list = analyticsService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.ok("Low-stock products fetched", list));
    }

    /**
     * Query 5: Categories Containing More Than a Specified Number of Products (GROUP BY & HAVING)
     */
    @GetMapping("/category-product-count")
    public ResponseEntity<ApiResponse<List<CategoryProductCountDto>>> getCategoriesByMinProducts(
            @RequestParam(defaultValue = "2") long minCount) {
        List<CategoryProductCountDto> list = analyticsService.getCategoriesByMinProducts(minCount);
        return ResponseEntity.ok(ApiResponse.ok("Categories with at least " + minCount + " products", list));
    }
}
