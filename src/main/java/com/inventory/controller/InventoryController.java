package com.inventory.controller;

import com.inventory.dto.*;
import com.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getAllInventory() {
        List<InventoryResponseDto> inventoryList = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.ok("Inventory list fetched successfully", inventoryList));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryByProductId(@PathVariable Long productId) {
        InventoryResponseDto inventory = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product inventory fetched successfully", inventory));
    }

    @PostMapping("/{productId}/stock-in")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> stockIn(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request) {
        InventoryResponseDto result = inventoryService.stockIn(productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock-in completed successfully", result));
    }

    @PostMapping("/{productId}/stock-out")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> stockOut(
            @PathVariable Long productId,
            @Valid @RequestBody StockOperationRequest request) {
        InventoryResponseDto result = inventoryService.stockOut(productId, request);
        return ResponseEntity.ok(ApiResponse.ok("Stock-out completed successfully", result));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockItemDto>>> getLowStock(
            @RequestParam(required = false) Integer threshold) {
        List<LowStockItemDto> lowStockList = inventoryService.getLowStockItems(threshold);
        return ResponseEntity.ok(ApiResponse.ok("Low-stock products fetched successfully", lowStockList));
    }

    @PatchMapping("/{productId}/threshold")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> updateThreshold(
            @PathVariable Long productId,
            @RequestParam int threshold) {
        InventoryResponseDto result = inventoryService.updateThreshold(productId, threshold);
        return ResponseEntity.ok(ApiResponse.ok("Low stock threshold updated successfully", result));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<StockTransactionDto>>> getRecentTransactions() {
        List<StockTransactionDto> transactions = inventoryService.getRecentTransactions();
        return ResponseEntity.ok(ApiResponse.ok("Recent stock transactions fetched successfully", transactions));
    }

    @GetMapping("/transactions/{productId}")
    public ResponseEntity<ApiResponse<List<StockTransactionDto>>> getTransactionsForProduct(@PathVariable Long productId) {
        List<StockTransactionDto> transactions = inventoryService.getTransactionsForProduct(productId);
        return ResponseEntity.ok(ApiResponse.ok("Transactions for product fetched successfully", transactions));
    }
}
