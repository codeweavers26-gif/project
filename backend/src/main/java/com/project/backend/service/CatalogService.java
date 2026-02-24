package com.project.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.repository.AttributeOptionRepository;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.SectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
   
    private final AttributeOptionRepository attributeOptionRepo;

    public List<Section> getActiveSections() {
        return sectionRepo.findByIsActiveTrue();
    }

    public List<Category> getCategoriesBySection(Long sectionId) {
        return categoryRepo.findByParentIdAndIsActiveTrue(sectionId);
    }

}
