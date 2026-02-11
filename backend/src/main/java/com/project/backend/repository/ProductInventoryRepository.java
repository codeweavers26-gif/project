package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Location;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;
@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

    Optional<ProductInventory> findByProductAndLocation(Product product, Location location);

    @Query("""
        SELECT pi FROM ProductInventory pi
        WHERE (:productId IS NULL OR pi.product.id = :productId)
          AND (:locationId IS NULL OR pi.location.id = :locationId)
    """)
    Page<ProductInventory> search(
        @Param("productId") Long productId,
        @Param("locationId") Long locationId,
        Pageable pageable
    );

    Page<ProductInventory> findByStockLessThan(int threshold, Pageable pageable);

    List<ProductInventory> findByStock(int stock);
}