package com.project.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.entity.Location;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

	Optional<ProductInventory> findByProductAndLocation(Product product, Location location);
}
