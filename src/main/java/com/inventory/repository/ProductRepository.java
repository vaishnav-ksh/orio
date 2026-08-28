package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    List<Product> findByCategoryId(Long categoryId);

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    List<Product> searchProducts(@Param("query") String query);

    // Query 1: Products with available stock
    @Query("""
        SELECT p FROM Product p
        INNER JOIN p.inventory i
        WHERE i.quantity > 0
        ORDER BY i.quantity DESC, p.name ASC
    """)
    List<Product> findProductsWithAvailableStock();

    // Query 2: Low-stock products based on product's threshold
    @Query("""
        SELECT p FROM Product p
        INNER JOIN p.inventory i
        WHERE i.quantity <= i.lowStockThreshold
        ORDER BY i.quantity ASC, p.name ASC
    """)
    List<Product> findLowStockProducts();

    // Low-stock products based on a custom threshold
    @Query("""
        SELECT p FROM Product p
        INNER JOIN p.inventory i
        WHERE i.quantity <= :threshold
        ORDER BY i.quantity ASC, p.name ASC
    """)
    List<Product> findProductsBelowStockThreshold(@Param("threshold") int threshold);
}
