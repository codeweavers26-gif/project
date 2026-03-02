package com.project.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
    @Query("SELECT c FROM Category c WHERE c.section.id = :sectionId")
    List<Category> findAllBySectionId(@Param("sectionId") Long sectionId);
  
    List<Category> findByIsActiveTrue();
    Optional<Category> findFirstBySectionId(Long sectionId);
    List<Category> findBySectionId(Long sectionId);
    boolean existsByName(String name);
    @Query("SELECT c FROM Category c WHERE c.section.id = :sectionId ORDER BY c.id LIMIT 1")
    Optional<Category> findFirstBySectionIdQuery(@Param("sectionId") Long sectionId);

    boolean existsBySlug(String slug);
    

    
    List<Category> findBySectionIdAndIsActiveTrue(Long sectionId);
    

}