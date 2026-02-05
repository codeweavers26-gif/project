package com.project.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.entity.AttributeOption;
import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.entity.SubCategory;
import com.project.backend.entity.SubCategoryAttribute;
import com.project.backend.repository.AttributeOptionRepository;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.repository.SubCategoryAttributeRepository;
import com.project.backend.repository.SubCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
    private final SubCategoryRepository subCategoryRepo;
    private final SubCategoryAttributeRepository subCategoryAttrRepo;
    private final AttributeOptionRepository attributeOptionRepo;

    public List<Section> getActiveSections() {
        return sectionRepo.findByIsActiveTrue();
    }

    public List<Category> getCategoriesBySection(Long sectionId) {
        return categoryRepo.findBySectionIdAndIsActiveTrue(sectionId);
    }

    public List<SubCategory> getSubCategories(Long categoryId) {
        return subCategoryRepo.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    public Map<String, List<String>> getFiltersForSubCategory(Long subCategoryId) {

        List<SubCategoryAttribute> attrs = subCategoryAttrRepo.findBySubCategoryId(subCategoryId);

        Map<String, List<String>> filters = new HashMap<>();

        for (SubCategoryAttribute sca : attrs) {
            String attrName = sca.getAttribute().getName();

            List<String> options = attributeOptionRepo
                    .findByAttributeId(sca.getAttribute().getId())
                    .stream()
                    .map(AttributeOption::getValue)
                    .toList();

            filters.put(attrName, options);
        }

        return filters;
    }
}
