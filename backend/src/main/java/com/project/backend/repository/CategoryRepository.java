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

    // ============= BASIC QUERIES =============
    
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
    
    // ============= HIERARCHY QUERIES =============
    
    // Get root categories (sections) - where parent is null
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true")
    List<Category> findRootCategories();
    
    // Get subcategories by parent ID
    List<Category> findByParentIdAndIsActiveTrue(Long parentId);
    
    // Get all categories in a hierarchy (for breadcrumb)
    @Query("SELECT c FROM Category c WHERE c.id = :id OR c.parent.id = :id OR c.parent.parent.id = :id")
    List<Category> findCategoryHierarchy(@Param("id") Long id);
    
    // ============= PAGINATION QUERIES =============
    
    // Get categories by parent ID with pagination
    Page<Category> findByParentId(Long parentId, Pageable pageable);
    
    // Get active categories by parent ID with pagination
    Page<Category> findByParentIdAndIsActiveTrue(Long parentId, Pageable pageable);
    
    // ============= VALIDATION QUERIES =============
    
    // Check if name exists under a specific parent
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.parent.id = :parentId")
    boolean existsByNameIgnoreCaseAndParentId(@Param("name") String name, @Param("parentId") Long parentId);
    
    // Find category by name and parent
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.parent.id = :parentId")
    Optional<Category> findByNameAndParentId(@Param("name") String name, @Param("parentId") Long parentId);
    
    // Count categories under a parent
    long countByParentId(Long parentId);
    
    // ============= ADDITIONAL QUERIES =============
    
    // Get all leaf categories (categories with no children)
    @Query("SELECT c FROM Category c WHERE c.subCategories IS EMPTY")
    List<Category> findLeafCategories();
    
    // Get category with its children
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subCategories WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(@Param("id") Long id);
    
    // ============= NOTE: The following methods have been REMOVED =============
    // They referenced 'sectionId' which doesn't exist in your Category entity
    // - findBySectionIdAndIsActiveTrue (removed)
    // - existsByNameIgnoreCaseAndSectionId (removed)
    // - findBySectionId(Long sectionId, Pageable pageable) (removed)
    // - findByNameAndSectionId(String name, Long sectionId) (removed)
    // - countBySectionId(Long sectionId) (removed)
}