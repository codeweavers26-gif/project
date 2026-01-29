package com.project.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.AttributeConfig;

@Repository
public interface AttributeConfigRepository extends JpaRepository<AttributeConfig, Long> {
    List<AttributeConfig> findByCategoryIdAndActiveTrue(Long categoryId);

	List<AttributeConfig> findByActiveTrue();
}


