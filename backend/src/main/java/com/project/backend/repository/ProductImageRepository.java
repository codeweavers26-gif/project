package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);
@Modifying
@Query("UPDATE ProductImage i SET i.isPrimary = false WHERE i.product.id = :productId AND (:excludeId IS NULL OR i.id != :excludeId)")
void unsetPrimaryForProduct(@Param("productId") Long productId, @Param("excludeId") Long excludeId);

Optional<ProductImage> findFirstByProductIdOrderByPositionAsc(Long productId);
    void deleteByProductId(Long productId);
	void deleteByProduct(Product product);}