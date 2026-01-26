package com.project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Product;
import com.project.backend.entity.ProductAttribute;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
	@Query("SELECT SUM(p.stock) FROM Product p WHERE p.isActive = true")
	Long getTotalStock();

	void deleteByProduct(Product product);}