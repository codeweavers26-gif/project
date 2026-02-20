package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.SubCategory;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    List<SubCategory> findByCategoryIdAndIsActiveTrue(Long categoryId);
    Page<SubCategory> findByCategoryId(Long categoryId, Pageable pageable);
    Optional<SubCategory> findByNameAndCategoryId(String name, Long categoryId);
    long countByCategoryId(Long categoryId);
	boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
}
