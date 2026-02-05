package com.project.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.SubCategoryAttribute;

@Repository
public interface SubCategoryAttributeRepository extends JpaRepository<SubCategoryAttribute, Long> {
    List<SubCategoryAttribute> findBySubCategoryId(Long subCategoryId);

	boolean existsBySubCategoryIdAndAttributeId(Long subCategoryId, Long attributeId);
}
