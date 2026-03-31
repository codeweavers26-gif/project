package com.project.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.CategoryResponseDto;
import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.requestDto.CategoryRequest;
import com.project.backend.requestDto.UpdateCategoryRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
   

    public List<Section> getActiveSections() {
        return sectionRepo.findByIsActiveTrue();
    }

    public List<CategoryResponseDto> getCategoriesBySection(Long sectionId) {
        Section section = sectionRepo.findById(sectionId)
            .orElseThrow(() -> new NotFoundException("Section not found"));
        
        if (!section.getIsActive()) {
            throw new BadRequestException("Section is not active");
        }
       
        List<Category> categories = categoryRepo.findBySectionIdAndIsActiveTrue(sectionId);
        
      
        return categories.stream()
            .map(this::mapToCategoryResponseDto)
            .collect(Collectors.toList());
    }
    
    private CategoryResponseDto mapToCategoryResponseDto(Category category) {
        return CategoryResponseDto.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .isActive(category.getIsActive())
            .sectionId(category.getSection() != null ? category.getSection().getId() : null)
            .build();
    }

  @Transactional
    public CategoryResponseDto createCategory(CategoryRequest req) {

        log.info("Creating category: {}", req.getName());

        Section section = sectionRepo.findById(req.getSectionId())
                .orElseThrow(() -> new NotFoundException("Section not found"));

        if (!section.getIsActive()) {
            throw new BadRequestException("Section inactive");
        }

        String slug = generateUniqueSlug(req.getName());

        Category category = new Category();
        category.setName(req.getName());
        category.setSlug(slug);
        category.setSection(section);
        category.setIsActive(true);

        categoryRepo.save(category);

        return map(category);
    }

    @Transactional
    public CategoryResponseDto updateCategory(Long id, UpdateCategoryRequest req) {

        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (req.getName() != null) {
            category.setName(req.getName());
            category.setSlug(generateUniqueSlug(req.getName()));
        }

        if (req.getSectionId() != null) {
            Section section = sectionRepo.findById(req.getSectionId())
                    .orElseThrow(() -> new NotFoundException("Section not found"));

            if (!section.getIsActive()) {
                throw new BadRequestException("Section inactive");
            }

            category.setSection(section);
        }

        if (req.getIsActive() != null) {
            category.setIsActive(req.getIsActive());
        }

        return map(category);
    }

    @Transactional
    public void deleteCategory(Long id) {

        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!category.getIsActive()) {
            throw new BadRequestException("Already deleted");
        }

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new BadRequestException("Category has products");
        }

        category.setIsActive(false);

        log.info("Category soft deleted: {}", id);
    }

private String generateUniqueSlug(String name) {

    String base = name.trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");

    String slug = base;

    for (int i = 0; i < 5; i++) {

        if (!categoryRepo.existsBySlug(slug)) {
            return slug;
        }

        slug = base + "-" + (i + 1);
    }

    return base + "-" + System.currentTimeMillis();
}
 private CategoryResponseDto map(Category c) {
        return CategoryResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .isActive(c.getIsActive())
                .sectionId(c.getSection().getId())
                .build();
    }
}
