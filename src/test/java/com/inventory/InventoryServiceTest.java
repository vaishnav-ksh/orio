package com.inventory;

import com.inventory.dto.InventoryResponseDto;
import com.inventory.dto.LowStockItemDto;
import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.dto.StockOperationRequest;
import com.inventory.dto.StockTransactionDto;
import com.inventory.entity.Category;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidOperationException;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    private ProductResponseDto testProduct;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(new Category("Warehousing", "Storage testing"));
        testProduct = productService.createProduct(new ProductRequestDto(
                "INV-TEST-001",
                "Warehouse Barcode Scanner",
                "Industrial 2D Barcode Scanner",
                new BigDecimal("120.00"),
                category.getId(),
                15, // initial stock
                5   // threshold
        ));
    }

    @Test
    @DisplayName("Stock-In: Successfully increments stock and creates transaction record")
    void shouldSuccessfullyStockIn() {
        StockOperationRequest request = new StockOperationRequest(10, "Shipment batch #901");
        InventoryResponseDto result = inventoryService.stockIn(testProduct.getId(), request);

        assertThat(result.getCurrentStock()).isEqualTo(25); // 15 + 10 = 25
        assertThat(result.getStockStatus()).isEqualTo("IN_STOCK");

        // Verify transaction logged
        List<StockTransactionDto> transactions = inventoryService.getTransactionsForProduct(testProduct.getId());
        assertThat(transactions).isNotEmpty();
        StockTransactionDto latest = transactions.get(0);
        assertThat(latest.getQuantity()).isEqualTo(10);
        assertThat(latest.getPreviousStock()).isEqualTo(15);
        assertThat(latest.getNewStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("Stock-Out: Successfully decrements stock when requested quantity <= available stock")
    void shouldSuccessfullyStockOut() {
        StockOperationRequest request = new StockOperationRequest(5, "Order fulfillment #123");
        InventoryResponseDto result = inventoryService.stockOut(testProduct.getId(), request);

        assertThat(result.getCurrentStock()).isEqualTo(10); // 15 - 5 = 10
        assertThat(result.getStockStatus()).isEqualTo("IN_STOCK");

        List<StockTransactionDto> transactions = inventoryService.getTransactionsForProduct(testProduct.getId());
        StockTransactionDto latest = transactions.get(0);
        assertThat(latest.getQuantity()).isEqualTo(5);
        assertThat(latest.getPreviousStock()).isEqualTo(15);
        assertThat(latest.getNewStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Business Rule: Stock-out quantity cannot exceed available stock -> throws InsufficientStockException")
    void shouldThrowInsufficientStockExceptionWhenStockOutExceedsAvailable() {
        // testProduct currently has 15 units. Requesting 20 units.
        StockOperationRequest request = new StockOperationRequest(20, "Excessive order request");

        assertThatThrownBy(() -> inventoryService.stockOut(testProduct.getId(), request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Cannot perform stock-out of 20 units")
                .hasMessageContaining("Available stock is only 15 units");

        // Verify stock is untouched
        InventoryResponseDto current = inventoryService.getInventoryByProductId(testProduct.getId());
        assertThat(current.getCurrentStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Stock-Out: Successfully reduces stock to exact zero and updates status to OUT_OF_STOCK")
    void shouldAllowStockOutToZero() {
        StockOperationRequest request = new StockOperationRequest(15, "Full inventory dispatch");
        InventoryResponseDto result = inventoryService.stockOut(testProduct.getId(), request);

        assertThat(result.getCurrentStock()).isEqualTo(0);
        assertThat(result.getStockStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Stock-In & Stock-Out with invalid non-positive quantities should be rejected")
    void shouldRejectInvalidStockQuantities() {
        StockOperationRequest zeroRequest = new StockOperationRequest(0, "Invalid zero");
        assertThatThrownBy(() -> inventoryService.stockIn(testProduct.getId(), zeroRequest))
                .isInstanceOf(InvalidOperationException.class);

        StockOperationRequest negativeRequest = new StockOperationRequest(-5, "Invalid negative");
        assertThatThrownBy(() -> inventoryService.stockOut(testProduct.getId(), negativeRequest))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Low-Stock threshold detection: Identifies items at or below configured threshold")
    void shouldIdentifyLowStockItems() {
        // Currently 15 units, threshold 5. Dispatch 11 units -> remaining 4 units (<= 5).
        inventoryService.stockOut(testProduct.getId(), new StockOperationRequest(11, "Stock drain"));

        List<LowStockItemDto> lowStockItems = inventoryService.getLowStockItems(null);
        boolean containsTestProduct = lowStockItems.stream()
                .anyMatch(item -> item.getProductId().equals(testProduct.getId()) && item.getCurrentStock() == 4);

        assertThat(containsTestProduct).isTrue();
    }

    @Test
    @DisplayName("Threshold Configuration: Successfully update threshold per product")
    void shouldUpdateConfigurableLowStockThreshold() {
        InventoryResponseDto updated = inventoryService.updateThreshold(testProduct.getId(), 20);
        assertThat(updated.getLowStockThreshold()).isEqualTo(20);
        assertThat(updated.getStockStatus()).isEqualTo("LOW_STOCK"); // 15 <= 20
    }
}
