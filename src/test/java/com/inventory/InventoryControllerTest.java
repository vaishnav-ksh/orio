package com.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.dto.StockOperationRequest;
import com.inventory.entity.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    private ProductResponseDto testProduct;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(new Category("Controller Inventory Category", "Testing stock API"));
        testProduct = productService.createProduct(new ProductRequestDto(
                "CTRL-INV-SKU",
                "Stock Controlled Item",
                "Testing stock-in and stock-out",
                new BigDecimal("25.00"),
                category.getId(),
                30, // 30 units initial
                10  // 10 threshold
        ));
    }

    @Test
    @DisplayName("POST /api/inventory/{productId}/stock-in adds stock")
    void shouldExecuteStockInEndpoint() throws Exception {
        StockOperationRequest request = new StockOperationRequest(15, "Receiving supplier batch");

        mockMvc.perform(post("/api/inventory/" + testProduct.getId() + "/stock-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStock").value(45)); // 30 + 15 = 45
    }

    @Test
    @DisplayName("POST /api/inventory/{productId}/stock-out deducts stock")
    void shouldExecuteStockOutEndpoint() throws Exception {
        StockOperationRequest request = new StockOperationRequest(10, "Sales order dispatch");

        mockMvc.perform(post("/api/inventory/" + testProduct.getId() + "/stock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStock").value(20)); // 30 - 10 = 20
    }

    @Test
    @DisplayName("POST /api/inventory/{productId}/stock-out with excessive quantity returns 400 Bad Request")
    void shouldReturnBadRequestOnInsufficientStock() throws Exception {
        // testProduct only has 30 units. Requesting 50 units.
        StockOperationRequest excessiveRequest = new StockOperationRequest(50, "Excessive order");

        mockMvc.perform(post("/api/inventory/" + testProduct.getId() + "/stock-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Insufficient Stock"))
                .andExpect(jsonPath("$.message", containsString("Cannot perform stock-out of 50 units")));
    }

    @Test
    @DisplayName("GET /api/inventory/low-stock returns low stock items")
    void shouldGetLowStockItems() throws Exception {
        mockMvc.perform(get("/api/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
