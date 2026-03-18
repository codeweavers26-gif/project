package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.WarehouseInventory;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {
 Long countByWarehouseId(Long warehouseId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT wi FROM WarehouseInventory wi WHERE wi.variant.id = :variantId AND wi.warehouse.id = :warehouseId")
	Optional<WarehouseInventory> findByVariantAndWarehouseForUpdate(@Param("variantId") Long variantId,
			@Param("warehouseId") Long warehouseId);

	@Query("""
			    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
			    WHERE wi.variant.product.id = :productId
			    AND wi.availableQuantity >= :minQuantity
			""")
	boolean existsByProductWithMinStock(@Param("productId") Long productId, @Param("minQuantity") int minQuantity);

	@Query("""
			    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
			    WHERE wi.variant.product.id = :productId
			    AND wi.availableQuantity >= :minQuantity
			""")
	boolean existsByProductAndMinQuantity(@Param("productId") Long productId, @Param("minQuantity") int minQuantity);

	Optional<WarehouseInventory> findByVariantIdAndWarehouseId(Long variantId, Long warehouseId);

	@Query("""
			    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
			    WHERE wi.variant.product.id = :productId
			    AND wi.availableQuantity > 0
			""")
	boolean existsByProductWithAnyStock(@Param("productId") Long productId);

	@Query("""
			    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
			    WHERE wi.variant.product.id = :productId
			    AND wi.availableQuantity > 0
			""")
	boolean existsByProductAndAvailableQuantityGreaterThan(@Param("productId") Long productId);
	@Query("""
	    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
	    WHERE wi.variant.product.id = :productId
	    AND wi.availableQuantity > 0
	""")
	boolean hasAnyStock(@Param("productId") Long productId);
	
	@Query("""
	        SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
	        WHERE wi.variant.product.id = :productId
	        AND wi.availableQuantity >= :minQuantity
	    """)
	    boolean existsByProductAndAvailableQuantityGreaterThan(
	        @Param("productId") Long productId, 
	        @Param("minQuantity") int minQuantity
	    );
	List<WarehouseInventory> findByVariantId(Long variantId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	  @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
	@Query("""
	    SELECT wi
	    FROM WarehouseInventory wi
	    WHERE wi.variant.id = :variantId
	""")
	List<WarehouseInventory> findByVariantIdForUpdate(@Param("variantId") Long variantId);
	
	
	  @Lock(LockModeType.PESSIMISTIC_WRITE)
	    @QueryHints({
	        @QueryHint(name = "javax.persistence.lock.timeout", value = "3000"),
	        @QueryHint(name = "org.hibernate.lockMode", value = "PESSIMISTIC_WRITE"),
	        @QueryHint(name = "javax.persistence.lock.scope", value = "EXTENDED")
	    })
	    @Query(value = "SELECT * FROM warehouse_inventory WHERE variant_id = :variantId FOR UPDATE SKIP LOCKED", 
	           nativeQuery = true)
	    List<WarehouseInventory> findByVariantIdWithSkipLocked(@Param("variantId") Long variantId);

	    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.variant.id = :variantId")
    List<WarehouseInventory> c(@Param("variantId") Long variantId);
    List<WarehouseInventory> findByVariant_IdIn(List<Long> variantIds);
}