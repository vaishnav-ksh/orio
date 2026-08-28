package com.inventory.config;

import com.inventory.dto.ProductRequestDto;
import com.inventory.entity.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedDatabase(CategoryRepository categoryRepository,
                                  ProductRepository productRepository,
                                  ProductService productService) {
        return args -> {
            if (categoryRepository.count() > 0) {
                log.info("Database already seeded. Skipping initial data load.");
                return;
            }

            log.info("Seeding initial inventory database with categories, products, and initial stock...");

            Category electronics = categoryRepository.save(new Category("Electronics", "Laptops, gadgets, audio gear, and peripherals"));
            Category furniture = categoryRepository.save(new Category("Furniture", "Ergonomic workspace seating, motorized desks, and lamps"));
            Category apparel = categoryRepository.save(new Category("Apparel", "Organic streetwear, performance jackets, and accessories"));
            Category groceries = categoryRepository.save(new Category("Groceries", "Specialty single-origin coffee beans, matcha, and pantry goods"));
            Category stationery = categoryRepository.save(new Category("Stationery", "Fountain pens, dot grid journals, and desk organizers"));

            // Seed sample products with various stock levels (normal, low stock, out of stock)
            productService.createProduct(new ProductRequestDto("ELEC-MBP-14", "MacBook Pro 14\" M3", "Apple Silicon M3 Pro, 18GB Unified Memory, 512GB SSD Space Black", new BigDecimal("1999.00"), electronics.getId(), 15, 5));
            productService.createProduct(new ProductRequestDto("ELEC-SNY-WH1000", "Sony WH-1000XM5 Headphones", "Flagship active noise canceling wireless over-ear headphones", new BigDecimal("349.99"), electronics.getId(), 4, 10)); // Low stock
            productService.createProduct(new ProductRequestDto("ELEC-LOG-MX3", "Logitech MX Master 3S", "Performance wireless ergonomic mouse with 8K DPI sensor", new BigDecimal("99.99"), electronics.getId(), 42, 10));
            productService.createProduct(new ProductRequestDto("ELEC-KBD-MECH", "Keychron Q1 Pro Mechanical Keyboard", "Wireless custom mechanical keyboard with hot-swappable switches", new BigDecimal("199.00"), electronics.getId(), 8, 10)); // Low stock

            productService.createProduct(new ProductRequestDto("FURN-CHAIR-ERG", "Ergohuman High-Back Chair", "Mesh executive task chair with 3D dynamic lumbar support", new BigDecimal("629.50"), furniture.getId(), 12, 5));
            productService.createProduct(new ProductRequestDto("FURN-DESK-STD", "ApexDesk Motorized Standing Desk", "60-inch dual-motor electric standing desk with memory presets", new BigDecimal("549.00"), furniture.getId(), 2, 5)); // Low stock
            productService.createProduct(new ProductRequestDto("FURN-LAMP-LED", "BenQ ScreenBar Monitor Light", "Auto-dimming eye-care LED monitor light bar with desktop dial", new BigDecimal("139.00"), furniture.getId(), 25, 8));

            productService.createProduct(new ProductRequestDto("APP-HOODIE-BLK", "Classic Organic Cotton Hoodie", "Heavyweight 450 GSM French terry hoodie in pitch black", new BigDecimal("78.00"), apparel.getId(), 50, 15));
            productService.createProduct(new ProductRequestDto("APP-JCKT-WTR", "Alpine Waterproof Shell Jacket", "3-layer breathable windproof mountain technical jacket", new BigDecimal("289.00"), apparel.getId(), 3, 10)); // Low stock

            productService.createProduct(new ProductRequestDto("GROC-COF-ETH", "Ethiopian Yirgacheffe Coffee 1kg", "Single-origin washed arabica whole bean specialty coffee", new BigDecimal("24.50"), groceries.getId(), 85, 20));
            productService.createProduct(new ProductRequestDto("GROC-TEA-MTCH", "Uji Ceremonial Grade Matcha 100g", "Authentic stone ground Japanese green tea powder from Kyoto", new BigDecimal("32.00"), groceries.getId(), 6, 15)); // Low stock

            productService.createProduct(new ProductRequestDto("STAT-NOTE-LEU", "Leuchtturm1917 Notebook A5", "Hardcover dot-grid 251 pages fountain pen proof notebook", new BigDecimal("22.50"), stationery.getId(), 110, 25));
            productService.createProduct(new ProductRequestDto("STAT-PEN-LAMY", "Lamy Safari Fountain Pen - Black", "Ergonomic fountain pen with fine stainless steel nib", new BigDecimal("29.90"), stationery.getId(), 0, 10)); // Out of stock
            productService.createProduct(new ProductRequestDto("STAT-ORG-DSK", "Oak Wood Modular Desk Organizer", "Solid European white oak modular tray organizer with brass accents", new BigDecimal("45.00"), stationery.getId(), 18, 10));

            log.info("Database seeding completed with 5 categories and 14 products!");
        };
    }
}
