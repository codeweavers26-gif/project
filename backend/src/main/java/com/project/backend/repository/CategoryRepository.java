package com.project.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.AttributeOption;
import com.project.backend.entity.Category;
import com.project.backend.entity.SubCategory;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findBySectionIdAndIsActiveTrue(Long sectionId);

	boolean existsByNameIgnoreCaseAndSectionId(String name, Long sectionId);
	
	// CategoryRepository
	Page<Category> findBySectionId(Long sectionId, Pageable pageable);

	// SubCategoryRepository

	// AttributeOptionRepository

}
