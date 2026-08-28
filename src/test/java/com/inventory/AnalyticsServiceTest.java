package com.inventory;

import com.inventory.dto.CategoryProductCountDto;
import com.inventory.dto.CategoryStockDto;
import com.inventory.dto.InventorySummaryDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Query 1 Test: Products with Available Stock returns products with quantity > 0 in descending order")
    void testProductsWithAvailableStock() {
        List<ProductResponseDto> available = analyticsService.getProductsWithAvailableStock();
        assertThat(available).isNotEmpty();
        for (ProductResponseDto p : available) {
            assertThat(p.getStockQuantity()).isGreaterThan(0);
        }
        // Verify ordering: descending by stock quantity
        for (int i = 0; i < available.size() - 1; i++) {
            assertThat(available.get(i).getStockQuantity())
                    .isGreaterThanOrEqualTo(available.get(i + 1).getStockQuantity());
        }
    }

    @Test
    @DisplayName("Query 2 Test: Low-Stock Products returns products whose stock <= lowStockThreshold")
    void testLowStockProducts() {
        List<ProductResponseDto> lowStock = analyticsService.getLowStockProducts();
        assertThat(lowStock).isNotEmpty();
        for (ProductResponseDto p : lowStock) {
            assertThat(p.getStockQuantity()).isLessThanOrEqualTo(p.getLowStockThreshold());
        }
    }

    @Test
    @DisplayName("Query 3 Test: Stock by Category aggregates products count, total quantity, and valuation")
    void testStockByCategory() {
        List<CategoryStockDto> list = analyticsService.getStockByCategory();
        assertThat(list).isNotEmpty();
        for (CategoryStockDto c : list) {
            assertThat(c.getCategoryId()).isNotNull();
            assertThat(c.getCategoryName()).isNotBlank();
            assertThat(c.getTotalProducts()).isGreaterThanOrEqualTo(0);
            assertThat(c.getTotalStockQuantity()).isGreaterThanOrEqualTo(0);
            assertThat(c.getCategoryInventoryValuation()).isNotNull();
        }
    }

    @Test
    @DisplayName("Query 4 Test: Total Inventory Valuation calculates warehouse-wide sums and prices")
    void testTotalInventorySummary() {
        InventorySummaryDto summary = analyticsService.getTotalInventorySummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalDistinctProducts()).isGreaterThan(0);
        assertThat(summary.getTotalUnitsInStock()).isGreaterThan(0);
        assertThat(summary.getTotalInventoryValuation()).isGreaterThan(BigDecimal.ZERO);
        assertThat(summary.getMaxProductPrice()).isGreaterThanOrEqualTo(summary.getMinProductPrice());
    }

    @Test
    @DisplayName("Query 5 Test: Categories containing more than N products with HAVING filter")
    void testCategoriesWithMinProducts() {
        List<CategoryProductCountDto> list = analyticsService.getCategoriesByMinProducts(2);
        assertThat(list).isNotEmpty();
        for (CategoryProductCountDto item : list) {
            assertThat(item.getProductCount()).isGreaterThanOrEqualTo(2);
        }
    }
}
