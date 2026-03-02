package com.project.backend.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Product;
import com.project.backend.entity.ProductVariant;
import com.project.backend.requestDto.VariantAvailabilityDto;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    // Find variant by SKU
    Optional<ProductVariant> findBySku(String sku);
    
    // Find all variants for a product
    List<ProductVariant> findByProductId(Long productId);
    
    // Check if variant exists with same size/color
    boolean existsByProductIdAndSizeAndColor(Long productId, String size, String color);
    
    // Check if variant exists with same size/color excluding a specific variant ID
    @Query("SELECT COUNT(v) > 0 FROM ProductVariant v WHERE v.product.id = :productId AND v.size = :size AND v.color = :color AND v.id != :excludeId")
    boolean existsByProductIdAndSizeAndColorAndIdNot(
        @Param("productId") Long productId, 
        @Param("size") String size, 
        @Param("color") String color, 
        @Param("excludeId") Long excludeId
    );
    
    @Query("""
    	    SELECT v.id, v.size, v.color, v.sku,
    	           CASE WHEN SUM(wi.availableQuantity) > 0 THEN true ELSE false END,
    	           COALESCE(SUM(wi.availableQuantity), 0),
    	           v.sellingPrice
    	    FROM ProductVariant v
    	    LEFT JOIN WarehouseInventory wi ON wi.variant.id = v.id
    	    WHERE v.product.id = :productId
    	    AND v.isActive = true
    	    GROUP BY v.id, v.size, v.color, v.sku, v.sellingPrice
    	""")
    	List<Object[]> findAvailabilityRaw(@Param("productId") Long productId);
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);
    
    @Query("SELECT DISTINCT v.size FROM ProductVariant v WHERE v.product.id = :productId AND v.isActive = true")
    List<String> findDistinctSizesByProductId(@Param("productId") Long productId);
    
    @Query("SELECT DISTINCT v.color FROM ProductVariant v WHERE v.product.id = :productId AND v.isActive = true")
    List<String> findDistinctColorsByProductId(@Param("productId") Long productId);
   
    
    @Query("SELECT DISTINCT v.size FROM ProductVariant v WHERE v.product.category.id = :categoryId AND v.size IS NOT NULL")
    List<String> findDistinctSizesByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT v.color FROM ProductVariant v WHERE v.product.category.id = :categoryId AND v.color IS NOT NULL")
    List<String> findDistinctColorsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT MIN(v.sellingPrice), MAX(v.sellingPrice) FROM ProductVariant v WHERE v.product.category.id = :categoryId")
    List<Object[]> findPriceRangeByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT MIN(v.sellingPrice), MAX(v.sellingPrice) FROM ProductVariant v")
    List<Object[]> findGlobalPriceRange();
    @Query("SELECT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH pv.inventories " +
           "WHERE pv.id = :variantId")
    Optional<ProductVariant> findByIdWithProductAndInventories(@Param("variantId") Long variantId);
    
    @Query("SELECT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH pv.inventories " +
           "WHERE pv.id = :variantId AND pv.product.id = :productId")
    Optional<ProductVariant> findByIdAndProductIdWithInventories(
        @Param("variantId") Long variantId, 
        @Param("productId") Long productId);
    
    
    @Query("SELECT pv FROM ProductVariant pv LEFT JOIN FETCH pv.inventories WHERE pv.id = :variantId")
    Optional<ProductVariant> findByIdWithInventories(@Param("variantId") Integer variantId);
 
}