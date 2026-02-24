package com.project.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.AttributeConfig;

@Repository
public interface AttributeConfigRepository extends JpaRepository<AttributeConfig, Long> {

	boolean existsByNameIgnoreCase(String name);
 //   List<AttributeConfig> findByCategoryIdAndActiveTrue(Long categoryId);
	Optional<AttributeConfig> findByName(String name);
	//List<AttributeConfig> findByActiveTrue();
}


