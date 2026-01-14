package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.entity.Location;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

	// 🔹 All inventories for a product (Admin view)
	List<ProductInventory> findByProduct(Product product);

	// 🔹 All inventories for a location (Warehouse view)
	List<ProductInventory> findByLocation(Location location);

	// 🔹 Low stock alert
	List<ProductInventory> findByStockLessThan(int threshold);

	Optional<ProductInventory> findByProductAndLocation(Product product, Location location);
}
