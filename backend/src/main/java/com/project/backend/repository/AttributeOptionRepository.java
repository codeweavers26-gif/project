package com.project.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.AttributeOption;

@Repository
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, Long> {

    List<AttributeOption> findByAttributeId(Long attributeId);

    List<AttributeOption> findByAttributeIdAndActiveTrue(Long attributeId);
    List<AttributeOption> findByAttributeIdAndActiveTrueOrderBySortOrder(Long attributeId);

	Page<AttributeOption> findByAttributeId(Long attributeId, Pageable pageable);
	boolean existsByValueIgnoreCaseAndAttributeId(String value, Long attributeId);
}
