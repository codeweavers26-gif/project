package com.project.backend.repository;

import java.util.List;

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
    
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "WHERE p.isActive = true " +
            "AND (:sectionId IS NULL OR p.subCategory.category.section.id = :sectionId) " +
            "AND (:categoryId IS NULL OR p.subCategory.category.id = :categoryId) " +
            "AND (:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:brand IS NULL OR p.brand = :brand)")
     Page<Product> filterProducts(
             @Param("sectionId") Long sectionId,
             @Param("categoryId") Long categoryId,
             @Param("subCategoryId") Long subCategoryId,
             @Param("minPrice") Double minPrice,
             @Param("maxPrice") Double maxPrice,
             @Param("brand") String brand,
             Pageable pageable);
     
     // Get all products in a section (including all categories and subcategories)
     @Query("SELECT p FROM Product p WHERE p.subCategory.category.section.id = :sectionId")
     List<Product> findBySectionId(@Param("sectionId") Long sectionId);
     
     // Get all products in a category (including all subcategories)
     @Query("SELECT p FROM Product p WHERE p.subCategory.category.id = :categoryId")
     List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
     @Query("SELECT DISTINCT p.brand FROM Product p WHERE " +
    	       "(:sectionId IS NULL OR p.subCategory.category.section.id = :sectionId) AND " +
    	       "(:categoryId IS NULL OR p.subCategory.category.id = :categoryId) AND " +
    	       "(:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId) AND " +
    	       "p.brand IS NOT NULL")
    	List<String> findDistinctBrandsByHierarchy(
    	        @Param("sectionId") Long sectionId,
    	        @Param("categoryId") Long categoryId,
    	        @Param("subCategoryId") Long subCategoryId);



   
    	
    	   @Query("SELECT DISTINCT p FROM Product p " +
    	           "WHERE p.isActive = true " +
    	           "AND (:sectionId IS NULL OR p.subCategory.category.section.id = :sectionId) " +
    	           "AND (:categoryId IS NULL OR p.subCategory.category.id = :categoryId) " +
    	           "AND (:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId) " +
    	           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
    	           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
    	           "AND (:brand IS NULL OR p.brand = :brand) " +
    	           "AND (:inStockOnly = false OR p.stock > 0) " +
    	           "AND (:minRating IS NULL OR p.averageRating >= :minRating) " +
    	           "AND (:codAvailable = false OR p.codAvailable = true)")
    	    Page<Product> filterProducts(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId,
    	            @Param("minPrice") Double minPrice,
    	            @Param("maxPrice") Double maxPrice,
    	            @Param("brand") String brand,
    	            @Param("inStockOnly") Boolean inStockOnly,
    	            @Param("minRating") Double minRating,
    	            @Param("codAvailable") Boolean codAvailable,
    	            Pageable pageable);

    

    	    // ============= PRICE RANGE =============
    	    @Query("SELECT MIN(p.price), MAX(p.price) FROM Product p WHERE " +
    	           "(:sectionId IS NULL OR p.subCategory.category.section.id = :sectionId) AND " +
    	           "(:categoryId IS NULL OR p.subCategory.category.id = :categoryId) AND " +
    	           "(:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId)")
    	    List<Object[]> findPriceRangeByHierarchy(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId);

    	    // Helper methods for price range
    	    default Double findMinPriceByHierarchy(Long sectionId, Long categoryId, Long subCategoryId) {
    	        List<Object[]> result = findPriceRangeByHierarchy(sectionId, categoryId, subCategoryId);
    	        if (result.isEmpty() || result.get(0)[0] == null) return 0.0;
    	        return (Double) result.get(0)[0];
    	    }

    	    default Double findMaxPriceByHierarchy(Long sectionId, Long categoryId, Long subCategoryId) {
    	        List<Object[]> result = findPriceRangeByHierarchy(sectionId, categoryId, subCategoryId);
    	        if (result.isEmpty() || result.get(0)[1] == null) return 100000.0;
    	        return (Double) result.get(0)[1];
    	    }

    	    // ============= SIZES (from ProductAttribute) =============
    	    @Query("SELECT DISTINCT pa.option.value FROM ProductAttribute pa " +
    	           "WHERE pa.attribute.name = 'size' " +
    	           "AND pa.product.isActive = true " +
    	           "AND (:sectionId IS NULL OR pa.product.subCategory.category.section.id = :sectionId) " +
    	           "AND (:categoryId IS NULL OR pa.product.subCategory.category.id = :categoryId) " +
    	           "AND (:subCategoryId IS NULL OR pa.product.subCategory.id = :subCategoryId)")
    	    List<String> findDistinctSizesByHierarchy(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId);

    	    // ============= COLORS (from ProductAttribute) =============
    	    @Query("SELECT DISTINCT pa.option.value FROM ProductAttribute pa " +
    	           "WHERE pa.attribute.name = 'color' " +
    	           "AND pa.product.isActive = true " +
    	           "AND (:sectionId IS NULL OR pa.product.subCategory.category.section.id = :sectionId) " +
    	           "AND (:categoryId IS NULL OR pa.product.subCategory.category.id = :categoryId) " +
    	           "AND (:subCategoryId IS NULL OR pa.product.subCategory.id = :subCategoryId)")
    	    List<String> findDistinctColorsByHierarchy(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId);

    	    // ============= COUNT PRODUCTS =============
    	    @Query("SELECT COUNT(p) FROM Product p WHERE " +
    	           "p.isActive = true " +
    	           "AND (:sectionId IS NULL OR p.subCategory.category.section.id = :sectionId) " +
    	           "AND (:categoryId IS NULL OR p.subCategory.category.id = :categoryId) " +
    	           "AND (:subCategoryId IS NULL OR p.subCategory.id = :subCategoryId)")
    	    Long countByHierarchy(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId);

    	    // ============= MATERIALS (if you have them) =============
    	    @Query("SELECT DISTINCT pa.option.value FROM ProductAttribute pa " +
    	           "WHERE pa.attribute.name = 'material' " +
    	           "AND pa.product.isActive = true " +
    	           "AND (:sectionId IS NULL OR pa.product.subCategory.category.section.id = :sectionId) " +
    	           "AND (:categoryId IS NULL OR pa.product.subCategory.category.id = :categoryId) " +
    	           "AND (:subCategoryId IS NULL OR pa.product.subCategory.id = :subCategoryId)")
    	    List<String> findDistinctMaterialsByHierarchy(
    	            @Param("sectionId") Long sectionId,
    	            @Param("categoryId") Long categoryId,
    	            @Param("subCategoryId") Long subCategoryId);

}

