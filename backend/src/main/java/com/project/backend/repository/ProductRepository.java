package com.project.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    @Query("""
    		SELECT p FROM Product p
    		JOIN ProductInventory pi ON p.id = pi.product.id
    		WHERE pi.location.id = :locationId
    		AND pi.stock > 0
    		AND p.isActive = true
    		""")
    		Page<Product> findAvailableProducts(
    		        @Param("locationId") Long locationId,
    		        Pageable pageable);
    
    @Query("SELECT SUM(p.stock) FROM Product p WHERE p.isActive = true")
    Long getTotalStock();

    Long countByStockLessThan(Integer stock);

}

