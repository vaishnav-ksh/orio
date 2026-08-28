package com.inventory.service;

import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.entity.Category;
import com.inventory.entity.Inventory;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.exception.DuplicateResourceException;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final int defaultLowStockThreshold;

    public ProductService(ProductRepository productRepository,
                          CategoryService categoryService,
                          InventoryRepository inventoryRepository,
                          StockTransactionRepository stockTransactionRepository,
                          @Value("${app.inventory.default-low-stock-threshold:10}") int defaultLowStockThreshold) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.inventoryRepository = inventoryRepository;
        this.stockTransactionRepository = stockTransactionRepository;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(String search, Long categoryId, Boolean inStockOnly) {
        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productRepository.searchProducts(search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (Boolean.TRUE.equals(inStockOnly)) {
            products = productRepository.findProductsWithAvailableStock();
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        return mapToResponseDto(getProductEntity(id));
    }

    public ProductResponseDto createProduct(ProductRequestDto dto) {
        // Business Rule: Price cannot be negative
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Product price cannot be negative");
        }

        String sku = dto.getSku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Product with SKU '" + sku + "' already exists");
        }

        Category category = categoryService.getCategoryEntity(dto.getCategoryId());

        Product product = new Product();
        product.setSku(sku);
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(category);

        int initialStock = dto.getInitialStock() != null ? dto.getInitialStock() : 0;
        if (initialStock < 0) {
            throw new InvalidOperationException("Initial stock cannot be negative");
        }

        int threshold = dto.getLowStockThreshold() != null ? dto.getLowStockThreshold() : defaultLowStockThreshold;
        if (threshold < 0) {
            throw new InvalidOperationException("Low stock threshold cannot be negative");
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(initialStock);
        inventory.setLowStockThreshold(threshold);
        if (initialStock > 0) {
            inventory.setLastRestockedAt(LocalDateTime.now());
        }

        product.setInventory(inventory);
        Product savedProduct = productRepository.save(product);

        // Record initial stock transaction if stock > 0
        if (initialStock > 0) {
            StockTransaction transaction = new StockTransaction(
                    savedProduct,
                    TransactionType.STOCK_IN,
                    initialStock,
                    0,
                    initialStock,
                    "Initial stock upon product creation"
            );
            stockTransactionRepository.save(transaction);
        }

        return mapToResponseDto(savedProduct);
    }

    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {
        // Business Rule: Price cannot be negative
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Product price cannot be negative");
        }

        Product product = getProductEntity(id);
        String sku = dto.getSku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateResourceException("Another product with SKU '" + sku + "' already exists");
        }

        Category category = categoryService.getCategoryEntity(dto.getCategoryId());

        product.setSku(sku);
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(category);

        if (product.getInventory() != null && dto.getLowStockThreshold() != null) {
            if (dto.getLowStockThreshold() < 0) {
                throw new InvalidOperationException("Low stock threshold cannot be negative");
            }
            product.getInventory().setLowStockThreshold(dto.getLowStockThreshold());
        }

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = getProductEntity(id);
        productRepository.delete(product);
    }

    public ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }

        Inventory inventory = product.getInventory();
        if (inventory == null && product.getId() != null) {
            inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        }

        if (inventory != null) {
            int qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int threshold = inventory.getLowStockThreshold() != null ? inventory.getLowStockThreshold() : defaultLowStockThreshold;
            dto.setStockQuantity(qty);
            dto.setLowStockThreshold(threshold);
            dto.setLastRestockedAt(inventory.getLastRestockedAt());
            dto.setTotalValuation(product.getPrice() != null ? product.getPrice().multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO);

            if (qty == 0) {
                dto.setStockStatus("OUT_OF_STOCK");
            } else if (qty <= threshold) {
                dto.setStockStatus("LOW_STOCK");
            } else {
                dto.setStockStatus("IN_STOCK");
            }
        } else {
            dto.setStockQuantity(0);
            dto.setLowStockThreshold(defaultLowStockThreshold);
            dto.setStockStatus("OUT_OF_STOCK");
            dto.setTotalValuation(BigDecimal.ZERO);
        }

        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }
}
