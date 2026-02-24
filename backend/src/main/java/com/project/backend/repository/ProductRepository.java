package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	// ============= BASIC QUERIES =============

	Page<Product> findByIsActiveTrue(Pageable pageable);

	Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

	Page<Product> findByStatus(String status, Pageable pageable);

	Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

	Optional<Product> findByIdAndIsActiveTrue(Long id);

	@EntityGraph(value = "product-with-details")
	Optional<Product> findBySlug(String slug);

	Optional<Product> findBySlugAndIsActiveTrue(String slug);

	boolean existsBySlug(String slug);

	// ============= STOCK AVAILABILITY QUERIES =============

	@Query("""
			    SELECT DISTINCT p FROM Product p
			    JOIN p.variants v
			    JOIN WarehouseInventory wi ON wi.variant.id = v.id
			    WHERE p.isActive = true
			    AND p.isDeleted = false
			    AND v.isActive = true
			    AND wi.availableQuantity > 0
			    AND (:categoryId IS NULL OR p.category.id = :categoryId)
			""")
	Page<Product> findActiveProductsWithStock(@Param("categoryId") Long categoryId, Pageable pageable);

	@Query("""
			    SELECT DISTINCT p FROM Product p
			    JOIN p.variants v
			    JOIN WarehouseInventory wi ON wi.variant.id = v.id
			    JOIN wi.warehouse w
			    WHERE w.locationId = :locationId
			    AND p.isActive = true
			    AND p.isDeleted = false
			    AND v.isActive = true
			    AND wi.availableQuantity > 0
			""")
	Page<Product> findAvailableInLocation(@Param("locationId") Long locationId, Pageable pageable);

	// ============= FILTERING QUERIES =============

	// 🔥 FIXED: Simplified filter using only category
	@Query("""
			    SELECT DISTINCT p FROM Product p
			    LEFT JOIN FETCH p.images
			    WHERE p.isActive = true
			    AND (:categoryId IS NULL OR p.category.id = :categoryId)
			    AND (:minPrice IS NULL OR p.price >= :minPrice)
			    AND (:maxPrice IS NULL OR p.price <= :maxPrice)
			    AND (:brand IS NULL OR p.brand = :brand)
			""")
	Page<Product> filterProducts(@Param("categoryId") Long categoryId, @Param("minPrice") Double minPrice,
			@Param("maxPrice") Double maxPrice, @Param("brand") String brand, Pageable pageable);

	// 🔥 FIXED: Get distinct brands by category
	@Query("""
			    SELECT DISTINCT p.brand FROM Product p
			    WHERE p.isActive = true
			    AND p.brand IS NOT NULL
			    AND (:categoryId IS NULL OR p.category.id = :categoryId)
			""")
	List<String> findDistinctBrandsByCategory(@Param("categoryId") Long categoryId);

	// 🔥 FIXED: Get price range by category
	@Query("""
			    SELECT MIN(p.price), MAX(p.price) FROM Product p
			    WHERE p.isActive = true
			    AND (:categoryId IS NULL OR p.category.id = :categoryId)
			""")
	List<Object[]> findPriceRangeByCategory(@Param("categoryId") Long categoryId);

	default Double findMinPriceByCategory(Long categoryId) {
		List<Object[]> result = findPriceRangeByCategory(categoryId);
		if (result.isEmpty() || result.get(0)[0] == null)
			return 0.0;
		return (Double) result.get(0)[0];
	}

	default Double findMaxPriceByCategory(Long categoryId) {
		List<Object[]> result = findPriceRangeByCategory(categoryId);
		if (result.isEmpty() || result.get(0)[1] == null)
			return 100000.0;
		return (Double) result.get(0)[1];
	}

	// ============= COUNT QUERIES =============

	@Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
	Long getTotalActiveProducts();

	@Query("""
			    SELECT COUNT(p) FROM Product p
			    WHERE p.isActive = true
			    AND (:categoryId IS NULL OR p.category.id = :categoryId)
			""")
	Long countByCategory(@Param("categoryId") Long categoryId);

	List<Product> findAllByCategoryId(Long categoryId);

	@Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
	Long countByCategoryId(@Param("categoryId") Long categoryId);

//    @Override
//    @EntityGraph(attributePaths = {"variants", "images", "variants.inventories"})
//    Optional<Product> findById(Long id);
//    
	@Query("""
			    SELECT p FROM Product p
			    LEFT JOIN FETCH p.variants v
			    LEFT JOIN FETCH v.inventories i
			    LEFT JOIN FETCH p.images img
			    WHERE p.id = :id
			""")
	Optional<Product> findByIdWithAllDetails(@Param("id") Long id);

	Optional<Product> findById(Long id);

	@Query(value = """
			-- First get product and category data (single row)
			SELECT
			    p.id as product_id,
			    p.name as product_name,
			    p.slug as product_slug,
			    p.brand as product_brand,
			    p.short_description,
			    p.description,
			    p.price,
			    p.stock as total_stock,
			    p.is_active,
			    p.created_at,
			    p.updated_at,

			    c.id as category_id,
			    c.name as category_name,
			    c.slug as category_slug

			FROM products p
			LEFT JOIN categories c ON p.category_id = c.id
			WHERE p.id = :productId
			""", nativeQuery = true)
	List<Object[]> findProductBasic(@Param("productId") Long productId);

	@Query(value = """
			-- Get all variants with their inventory (no duplicates)
			SELECT
			    v.id as variant_id,
			    v.sku as variant_sku,
			    v.size as variant_size,
			    v.color as variant_color,
			    v.mrp as variant_mrp,
			    v.selling_price as variant_selling_price,
			    v.cost_price as variant_cost_price,
			    v.is_active as variant_is_active,
			    COALESCE(SUM(wi.available_quantity), 0) as available_stock,
			    COALESCE(SUM(wi.reserved_quantity), 0) as reserved_stock,
			    w.id as warehouse_id,
			    w.name as warehouse_name
			FROM product_variants v
			LEFT JOIN warehouse_inventory wi ON v.id = wi.variant_id
			LEFT JOIN warehouses w ON wi.warehouse_id = w.id
			WHERE v.product_id = :productId
			GROUP BY v.id, v.sku, v.size, v.color, v.mrp, v.selling_price, v.cost_price, v.is_active, w.id, w.name
			ORDER BY v.id
			""", nativeQuery = true)
	List<Object[]> findVariantsByProductId(@Param("productId") Long productId);

	@Query(value = """
			-- Get all images (no duplicates)
			SELECT
			    i.id as image_id,
			    i.image_url as image_url,
			    i.is_primary as image_primary,
			    i.position as image_position
			FROM product_images i
			WHERE i.product_id = :productId
			ORDER BY i.position ASC
			""", nativeQuery = true)
	List<Object[]> findImagesByProductId(@Param("productId") Long productId);

	@Query(value = """
			SELECT DISTINCT p FROM Product p
			WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
			AND (:status IS NULL OR p.status = :status)
			""", countQuery = """
			SELECT COUNT(DISTINCT p) FROM Product p
			WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
			AND (:status IS NULL OR p.status = :status)
			""")
	Page<Product> findProductsByFilters(@Param("categoryId") Long categoryId, @Param("status") String status,
			Pageable pageable);

	@Query(value = """
			SELECT DISTINCT
			    p.id,
			    p.name,
			    p.slug,
			    p.brand,
			    p.short_description,
			    p.price,
			    p.stock,
			    p.is_active,
			    (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
			    MIN(v.selling_price) as min_price,
			    c.id as category_id,
			    c.name as category_name,
			    c.slug as category_slug
			FROM products p
			LEFT JOIN categories c ON p.category_id = c.id
			LEFT JOIN product_variants v ON p.id = v.product_id
			LEFT JOIN warehouse_inventory wi ON v.id = wi.variant_id
			WHERE p.is_active = true
			AND p.is_deleted = false
			AND v.is_active = true
			AND wi.available_quantity > 0
			AND (:categoryId IS NULL OR p.category_id = :categoryId)
			AND (:minPrice IS NULL OR v.selling_price >= :minPrice)
			AND (:maxPrice IS NULL OR v.selling_price <= :maxPrice)
			AND (:size IS NULL OR v.size = :size)
			AND (:color IS NULL OR v.color = :color)
			AND (:brand IS NULL OR p.brand = :brand)
			GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug
			""", countQuery = """
			SELECT COUNT(DISTINCT p.id)
			FROM products p
			LEFT JOIN product_variants v ON p.id = v.product_id
			LEFT JOIN warehouse_inventory wi ON v.id = wi.variant_id
			WHERE p.is_active = true
			AND p.is_deleted = false
			AND v.is_active = true
			AND wi.available_quantity > 0
			AND (:categoryId IS NULL OR p.category_id = :categoryId)
			AND (:minPrice IS NULL OR v.selling_price >= :minPrice)
			AND (:maxPrice IS NULL OR v.selling_price <= :maxPrice)
			AND (:size IS NULL OR v.size = :size)
			AND (:color IS NULL OR v.color = :color)
			AND (:brand IS NULL OR p.brand = :brand)
			""", nativeQuery = true)
	Page<Object[]> findActiveProductsWithFilters(@Param("categoryId") Long categoryId,
			@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, @Param("size") String size,
			@Param("color") String color, @Param("brand") String brand, Pageable pageable);

	@Query(value = """
		    SELECT DISTINCT 
		         p.id, 
                    p.status,
                    p.description,
                    p.mrp,
                    p.returnable,
			        p.name, 
			        p.slug, 
			        p.brand, 
			        p.short_description,
			        p.price, 
			        p.stock, 
			        p.is_active,
			        (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
			        MIN(v.selling_price) as min_price,
			        c.id, 
			        c.name, 
			        c.slug
		    FROM products p
		    LEFT JOIN categories c ON p.category_id = c.id
		    JOIN product_variants v ON p.id = v.product_id AND v.is_active = true
		    JOIN warehouse_inventory wi ON v.id = wi.variant_id AND wi.available_quantity > 0
		    WHERE p.is_active = true 
		    AND p.is_deleted = false
		    AND v.is_active = true 
		    AND wi.available_quantity > 0
		    GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug
		    """, nativeQuery = true)
		Page<Object[]> findFeaturedProducts(Pageable pageable);

	@Query(value = """
		    SELECT DISTINCT 
		       p.id, 
                    p.status,
                    p.description,
                    p.mrp,
                    p.returnable,
			        p.name, 
			        p.slug, 
			        p.brand, 
			        p.short_description,
			        p.price, 
			        p.stock, 
			        p.is_active,
			        (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
			        MIN(v.selling_price) as min_price,
			        c.id, 
			        c.name, 
			        c.slug
		    FROM products p
		    LEFT JOIN categories c ON p.category_id = c.id
		    JOIN product_variants v ON p.id = v.product_id AND v.is_active = true
		    JOIN warehouse_inventory wi ON v.id = wi.variant_id AND wi.available_quantity > 0
		    WHERE p.is_active = true 
		    AND p.is_deleted = false
		    GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug

		    """, nativeQuery = true)
		Page<Object[]> findNewArrivals(Pageable pageable);

		@Query(value = """
			  SELECT DISTINCT 
			        p.id, 
                    p.status,
                    p.description,
                    p.mrp,
                    p.returnable,
			        p.name, 
			        p.slug, 
			        p.brand, 
			        p.short_description,
			        p.price, 
			        p.stock, 
			        p.is_active,
			        (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
			        MIN(v.selling_price) as min_price,
			        c.id, 
			        c.name, 
			        c.slug
			    FROM products p
			    LEFT JOIN categories c ON p.category_id = c.id
			    JOIN product_variants v ON p.id = v.product_id AND v.is_active = true
			    JOIN warehouse_inventory wi ON v.id = wi.variant_id AND wi.available_quantity > 0
			    WHERE p.is_active = true 
			    AND p.is_deleted = false
			    GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug
	
			    """, nativeQuery = true)
			Page<Object[]> findBestSellers(Pageable pageable);

	@Query(value = """
			SELECT DISTINCT
			      p.id, 
                    p.status,
                    p.description,
                    p.mrp,
                    p.returnable,
			        p.name, 
			        p.slug, 
			        p.brand, 
			        p.short_description,
			        p.price, 
			        p.stock, 
			        p.is_active,
			        (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
			        MIN(v.selling_price) as min_price,
			        c.id, 
			        c.name, 
			        c.slug
			FROM products p
			LEFT JOIN categories c ON p.category_id = c.id
			JOIN product_variants v ON p.id = v.product_id
			JOIN warehouse_inventory wi ON v.id = wi.variant_id
			WHERE p.category_id = (SELECT category_id FROM products WHERE id = :productId)
			AND p.id != :productId
			AND p.is_active = true
			AND p.is_deleted = false
			AND v.is_active = true
			AND wi.available_quantity > 0
			GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug
			""", nativeQuery = true)
	Page<Object[]> findRelatedProducts(@Param("productId") Long productId, Pageable pageable);
	
	@Query("SELECT DISTINCT p.brand FROM Product p WHERE p.category.id = :categoryId AND p.brand IS NOT NULL")
	List<String> findDistinctBrandsByCategoryId(@Param("categoryId") Long categoryId);
	@Query(value = """
		    SELECT DISTINCT 
		        p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active,
		        (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id ORDER BY i.position LIMIT 1) as thumbnail,
		        MIN(v.selling_price) as min_price,
		        c.id, c.name, c.slug
		    FROM products p
		    LEFT JOIN categories c ON p.category_id = c.id
		    LEFT JOIN product_variants v ON p.id = v.product_id AND v.is_active = true
		    LEFT JOIN warehouse_inventory wi ON v.id = wi.variant_id AND wi.available_quantity > 0
		    WHERE p.is_active = true 
		    AND p.is_deleted = false
		    AND (:categoryId IS NULL OR p.category_id = :categoryId)
		    AND (:minPrice IS NULL OR v.selling_price >= :minPrice)
		    AND (:maxPrice IS NULL OR v.selling_price <= :maxPrice)
		    AND (:size IS NULL OR v.size = :size)
		    AND (:color IS NULL OR v.color = :color)
		    AND (:brand IS NULL OR p.brand = :brand)
		    GROUP BY p.id, p.name, p.slug, p.brand, p.short_description, p.price, p.stock, p.is_active, c.id, c.name, c.slug
		    ORDER BY p.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(DISTINCT p.id)
		    FROM products p
		    LEFT JOIN product_variants v ON p.id = v.product_id AND v.is_active = true
		    LEFT JOIN warehouse_inventory wi ON v.id = wi.variant_id AND wi.available_quantity > 0
		    WHERE p.is_active = true 
		    AND p.is_deleted = false
		    AND (:categoryId IS NULL OR p.category_id = :categoryId)
		    AND (:minPrice IS NULL OR v.selling_price >= :minPrice)
		    AND (:maxPrice IS NULL OR v.selling_price <= :maxPrice)
		    AND (:size IS NULL OR v.size = :size)
		    AND (:color IS NULL OR v.color = :color)
		    AND (:brand IS NULL OR p.brand = :brand)
		    """,
		    nativeQuery = true)
		Page<Object[]> findActiveProductsWithFiltersNative(
		        @Param("categoryId") Long categoryId,
		        @Param("minPrice") Double minPrice,
		        @Param("maxPrice") Double maxPrice,
		        @Param("size") String size,
		        @Param("color") String color,
		        @Param("brand") String brand,
		        Pageable pageable);
	
}