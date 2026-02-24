package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Warehouse;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	// Find default warehouse (for Delhi launch)
	@Query("SELECT w FROM Warehouse w WHERE w.isDefault = true AND w.isActive = true")
	Optional<Warehouse> findDefaultWarehouse();

	// Find by location IDs
	Optional<Warehouse> findByLocationId(Long locationId);

	// Check if location exists
	boolean existsByLocationId(Long locationId);

	// Find all active warehouses
	List<Warehouse> findByIsActiveTrue();

	// Find warehouses in specific city/state
	@Query("SELECT w FROM Warehouse w WHERE w.city = :city AND w.isActive = true")
	List<Warehouse> findByCity(@Param("city") String city);

	@Query("SELECT w FROM Warehouse w WHERE w.state = :state AND w.isActive = true")
	List<Warehouse> findByState(@Param("state") String state);

	// Find warehouses with pagination
	Page<Warehouse> findByIsActiveTrue(Pageable pageable);

	// Find nearest warehouse by pincode (simplified for launch)
	@Query("""
			    SELECT w FROM Warehouse w
			    WHERE w.pincode = :pincode
			    AND w.isActive = true
			""")
	Optional<Warehouse> findByPincode(@Param("pincode") String pincode);

	// For multi-warehouse selection logic
	@Query("""
			    SELECT w FROM Warehouse w
			    WHERE w.isActive = true
			    ORDER BY
			        CASE WHEN w.state = :state THEN 0 ELSE 1 END,
			        CASE WHEN w.city = :city THEN 0 ELSE 1 END,
			        w.id
			""")
	List<Warehouse> findNearestWarehouses(@Param("state") String state, @Param("city") String city, Pageable pageable);

	// Check if warehouse has inventory for a variant
	@Query("""
			    SELECT COUNT(wi) > 0 FROM WarehouseInventory wi
			    WHERE wi.warehouse.id = :warehouseId
			    AND wi.variant.id = :variantId
			    AND wi.availableQuantity > 0
			""")
	boolean hasVariantInStock(@Param("warehouseId") Long warehouseId, @Param("variantId") Long variantId);
}