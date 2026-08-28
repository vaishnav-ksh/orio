package com.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.ProductRequestDto;
import com.inventory.entity.Category;
import com.inventory.repository.CategoryRepository;
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
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(new Category("Controller Test Category", "For MockMvc testing"));
    }

    @Test
    @DisplayName("GET /api/products returns 200 OK and product list")
    void shouldGetAllProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/products with valid payload returns 201 CREATED")
    void shouldCreateProduct() throws Exception {
        ProductRequestDto request = new ProductRequestDto(
                "CTRL-PROD-01",
                "Controller Test Item",
                "Product created via controller test",
                new BigDecimal("89.99"),
                testCategory.getId(),
                50,
                10
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("CTRL-PROD-01"))
                .andExpect(jsonPath("$.data.name").value("Controller Test Item"))
                .andExpect(jsonPath("$.data.price").value(89.99))
                .andExpect(jsonPath("$.data.stockQuantity").value(50));
    }

    @Test
    @DisplayName("POST /api/products with negative price returns 400 BAD REQUEST")
    void shouldRejectNegativePriceInController() throws Exception {
        ProductRequestDto request = new ProductRequestDto(
                "CTRL-NEG-PRICE",
                "Negative Price Item",
                "Should fail validation",
                new BigDecimal("-5.00"),
                testCategory.getId(),
                10,
                5
        );

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} deletes product successfully")
    void shouldDeleteProduct() throws Exception {
        ProductRequestDto request = new ProductRequestDto(
                "DEL-PROD-01",
                "Item to Delete",
                "Will be deleted",
                new BigDecimal("10.00"),
                testCategory.getId(),
                5,
                2
        );

        String responseJson = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long createdId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/products/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products/" + createdId))
                .andExpect(status().isNotFound());
    }
}
