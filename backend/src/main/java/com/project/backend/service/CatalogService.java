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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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

}
