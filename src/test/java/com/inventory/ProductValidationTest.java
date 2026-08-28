package com.inventory;

import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.entity.Category;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.InvalidOperationException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductValidationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(new Category("Test Category", "For testing validation"));
    }

    @Test
    @DisplayName("Should successfully create a product with valid positive price and initial stock")
    void shouldCreateProductWithValidPrice() {
        ProductRequestDto dto = new ProductRequestDto(
                "TEST-SKU-001",
                "Ergonomic Gaming Mouse",
                "RGB High Precision Wireless Gaming Mouse",
                new BigDecimal("49.99"),
                testCategory.getId(),
                20,
                5
        );

        ProductResponseDto created = productService.createProduct(dto);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSku()).isEqualTo("TEST-SKU-001");
        assertThat(created.getName()).isEqualTo("Ergonomic Gaming Mouse");
        assertThat(created.getPrice()).isEqualByComparingTo("49.99");
        assertThat(created.getStockQuantity()).isEqualTo(20);
        assertThat(created.getStockStatus()).isEqualTo("IN_STOCK");
    }

    @Test
    @DisplayName("Should allow creating product with zero price (free items/promotions)")
    void shouldAllowZeroPriceProduct() {
        ProductRequestDto dto = new ProductRequestDto(
                "TEST-FREE-001",
                "Promotional Sticker Pack",
                "Free stickers with order",
                BigDecimal.ZERO,
                testCategory.getId(),
                100,
                10
        );

        ProductResponseDto created = productService.createProduct(dto);
        assertThat(created.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Business Rule: Product price cannot be negative -> throws InvalidOperationException")
    void shouldRejectNegativePrice() {
        ProductRequestDto dto = new ProductRequestDto(
                "TEST-NEG-001",
                "Invalid Product",
                "Should fail due to negative price",
                new BigDecimal("-19.99"),
                testCategory.getId(),
                10,
                5
        );

        assertThatThrownBy(() -> productService.createProduct(dto))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Product price cannot be negative");
    }

    @Test
    @DisplayName("Business Rule: Rejects negative initial stock")
    void shouldRejectNegativeInitialStock() {
        ProductRequestDto dto = new ProductRequestDto(
                "TEST-NEG-STOCK",
                "Invalid Stock Product",
                "Should fail due to negative stock",
                new BigDecimal("10.00"),
                testCategory.getId(),
                -5,
                5
        );

        assertThatThrownBy(() -> productService.createProduct(dto))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Initial stock cannot be negative");
    }

    @Test
    @DisplayName("Business Rule: SKU must be unique -> throws DuplicateResourceException")
    void shouldRejectDuplicateSku() {
        ProductRequestDto dto1 = new ProductRequestDto(
                "TEST-UNIQUE-SKU",
                "First Product",
                "First",
                new BigDecimal("15.00"),
                testCategory.getId(),
                10,
                2
        );
        productService.createProduct(dto1);

        ProductRequestDto dto2 = new ProductRequestDto(
                "TEST-UNIQUE-SKU",
                "Second Product With Same SKU",
                "Second",
                new BigDecimal("25.00"),
                testCategory.getId(),
                5,
                2
        );

        assertThatThrownBy(() -> productService.createProduct(dto2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
