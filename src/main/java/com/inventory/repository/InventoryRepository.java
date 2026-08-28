package com.inventory.repository;

import com.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold ORDER BY i.quantity ASC")
    List<Inventory> findAllLowStock();

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold ORDER BY i.quantity ASC")
    List<Inventory> findByQuantityLessThanEqual(@Param("threshold") Integer threshold);

    @Query("SELECT i FROM Inventory i WHERE i.quantity > 0 ORDER BY i.quantity DESC")
    List<Inventory> findWithAvailableStock();

    // Query 4: Summary totals across all inventory
    @Query("""
        SELECT 
            COUNT(p.id),
            COALESCE(SUM(i.quantity), 0L),
            COALESCE(SUM(CASE WHEN i.quantity <= i.lowStockThreshold AND i.quantity > 0 THEN 1 ELSE 0 END), 0L),
            COALESCE(SUM(CASE WHEN i.quantity = 0 THEN 1 ELSE 0 END), 0L),
            COALESCE(MIN(p.price), 0.0),
            COALESCE(MAX(p.price), 0.0),
            COALESCE(AVG(p.price), 0.0),
            COALESCE(SUM(p.price * i.quantity), 0.0)
        FROM Product p
        INNER JOIN p.inventory i
    """)
    Object[] getInventoryOverallSummary();
}
