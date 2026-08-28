package com.inventory.repository;

import com.inventory.dto.CategoryProductCountDto;
import com.inventory.dto.CategoryStockDto;
import com.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Query 3: Stock by Category using JOIN, GROUP BY, SUM, AVG, COUNT, ORDER BY
    @Query("""
        SELECT new com.inventory.dto.CategoryStockDto(
            c.id,
            c.name,
            COUNT(p.id),
            COALESCE(SUM(i.quantity), 0L),
            COALESCE(AVG(i.quantity), 0.0),
            COALESCE(SUM(p.price * i.quantity), 0.0)
        )
        FROM Category c
        LEFT JOIN c.products p
        LEFT JOIN p.inventory i
        GROUP BY c.id, c.name
        ORDER BY COALESCE(SUM(i.quantity), 0L) DESC, c.name ASC
    """)
    List<CategoryStockDto> getStockByCategory();

    // Query 5: Categories containing more than a specified number of products using GROUP BY and HAVING COUNT(p.id) >= :minCount
    @Query("""
        SELECT new com.inventory.dto.CategoryProductCountDto(
            c.id,
            c.name,
            COUNT(p.id),
            COALESCE(SUM(i.quantity), 0L),
            COALESCE(SUM(p.price * i.quantity), 0.0)
        )
        FROM Category c
        INNER JOIN c.products p
        INNER JOIN p.inventory i
        GROUP BY c.id, c.name
        HAVING COUNT(p.id) >= :minCount
        ORDER BY COUNT(p.id) DESC, COALESCE(SUM(p.price * i.quantity), 0.0) DESC
    """)
    List<CategoryProductCountDto> findCategoriesWithProductCountGreaterThanEqual(@Param("minCount") long minCount);
}
