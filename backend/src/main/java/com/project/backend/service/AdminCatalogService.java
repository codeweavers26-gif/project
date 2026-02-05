package com.project.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.AdminCategoryResponseDto;
import com.project.backend.ResponseDto.AdminSectionResponseDto;
import com.project.backend.ResponseDto.AdminSubCategoryResponseDto;
import com.project.backend.ResponseDto.AminAttributeResponseDto;
import com.project.backend.ResponseDto.AttributeOptionResponseDto;
import com.project.backend.entity.AttributeConfig;
import com.project.backend.entity.AttributeOption;
import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.entity.SubCategory;
import com.project.backend.entity.SubCategoryAttribute;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.AttributeConfigRepository;
import com.project.backend.repository.AttributeOptionRepository;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.repository.SubCategoryAttributeRepository;
import com.project.backend.repository.SubCategoryRepository;
import com.project.backend.requestDto.AttributeOptionRequestDto;
import com.project.backend.requestDto.AttributeRequestDto;
import com.project.backend.requestDto.CategoryRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.SectionRequestDto;
import com.project.backend.requestDto.SubCategoryRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCatalogService {

    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
    private final SubCategoryRepository subCategoryRepo;
    private final AttributeConfigRepository attributeRepo;
    private final AttributeOptionRepository attributeOptionRepo;
    private final SubCategoryAttributeRepository subCategoryAttrRepo;

    // =========================================================
    // SECTION
    // =========================================================
    public Section createSection(SectionRequestDto dto) {

        if (sectionRepo.existsByNameIgnoreCase(dto.getName())) {
            throw new BadRequestException("Section already exists");
        }

        Section section = Section.builder()
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .isActive( true)
                .build();

        return sectionRepo.save(section);
    }

    // =========================================================
    // CATEGORY
    // =========================================================
    public Category createCategory(CategoryRequestDto dto) {

        Section section = sectionRepo.findById(dto.getSectionId())
                .orElseThrow(() -> new NotFoundException("Section not found"));

        if (categoryRepo.existsByNameIgnoreCaseAndSectionId(dto.getName(), dto.getSectionId())) {
            throw new BadRequestException("Category already exists in this section");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .section(section)
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return categoryRepo.save(category);
    }

    // =========================================================
    // SUBCATEGORY
    // =========================================================
    public SubCategory createSubCategory(SubCategoryRequestDto dto) {

        Category category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (subCategoryRepo.existsByNameIgnoreCaseAndCategoryId(dto.getName(), dto.getCategoryId())) {
            throw new BadRequestException("Subcategory already exists in this category");
        }

        SubCategory subCategory = SubCategory.builder()
                .name(dto.getName())
                .category(category)
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return subCategoryRepo.save(subCategory);
    }

    // =========================================================
    // ATTRIBUTE CONFIG (Size, Color, Fabric)
    // =========================================================
    public AttributeConfig createAttribute(AttributeRequestDto dto) {

        if (attributeRepo.existsByNameIgnoreCase(dto.getName())) {
            throw new BadRequestException("Attribute already exists");
        }
        Category category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        AttributeConfig attribute = AttributeConfig.builder()
                .name(dto.getName())
                .filterable(dto.getFilterable() != null ? dto.getFilterable() : true)
                .required(true)
                .category(category)   
                .build();

        return attributeRepo.save(attribute);
    }

    // =========================================================
    // ATTRIBUTE OPTIONS (Red, Blue, XL)
    // =========================================================
    public AttributeOption addAttributeOption(Long attributeId, AttributeOptionRequestDto dto) {

        AttributeConfig attribute = attributeRepo.findById(attributeId)
                .orElseThrow(() -> new NotFoundException("Attribute not found"));

        if (attributeOptionRepo.existsByValueIgnoreCaseAndAttributeId(dto.getValue(), attributeId)) {
            throw new BadRequestException("Option already exists for this attribute");
        }

        AttributeOption option = AttributeOption.builder()
                .value(dto.getValue())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .attribute(attribute)
                .build();

        return attributeOptionRepo.save(option);
    }

    // =========================================================
    // ASSIGN ATTRIBUTE TO SUBCATEGORY
    // =========================================================
    public SubCategoryAttribute assignAttributeToSubCategory(Long subCategoryId, Long attributeId) {

        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("Subcategory not found"));

        AttributeConfig attribute = attributeRepo.findById(attributeId)
                .orElseThrow(() -> new NotFoundException("Attribute not found"));

        if (subCategoryAttrRepo.existsBySubCategoryIdAndAttributeId(subCategoryId, attributeId)) {
            throw new BadRequestException("Attribute already assigned to this subcategory");
        }

        SubCategoryAttribute sca = SubCategoryAttribute.builder()
                .subCategory(subCategory)
                .attribute(attribute)
                .build();

        return subCategoryAttrRepo.save(sca);
    }
    
    
    public PageResponseDto<AdminSectionResponseDto> getSections(int page, int size) {

        Page<Section> sectionPage = sectionRepo.findAll(
                PageRequest.of(page, size, Sort.by("id").descending()));

        return PageResponseDto.<AdminSectionResponseDto>builder()
                .content(sectionPage.getContent().stream()
                        .map(s -> new AdminSectionResponseDto(
                                s.getId(), s.getName(), s.getImageUrl(), s.getIsActive()))
                        .toList())
                .page(sectionPage.getNumber())
                .size(sectionPage.getSize())
                .totalElements(sectionPage.getTotalElements())
                .totalPages(sectionPage.getTotalPages())
                .last(sectionPage.isLast())
                .build();
    }
    public PageResponseDto<AdminCategoryResponseDto> getCategoriesBySection(Long sectionId, int page, int size) {

        Page<Category> categoryPage = categoryRepo.findBySectionId(
                sectionId, PageRequest.of(page, size, Sort.by("id").descending()));

        return PageResponseDto.<AdminCategoryResponseDto>builder()
                .content(categoryPage.getContent().stream()
                        .map(c -> new AdminCategoryResponseDto(
                                c.getId(), c.getName(), c.getIsActive()))
                        .toList())
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .last(categoryPage.isLast())
                .build();
    }
    public PageResponseDto<AdminSubCategoryResponseDto> getSubCategoriesByCategory(Long categoryId, int page, int size) {

        Page<SubCategory> subCategoryPage = subCategoryRepo.findByCategoryId(
                categoryId, PageRequest.of(page, size, Sort.by("id").descending()));

        return PageResponseDto.<AdminSubCategoryResponseDto>builder()
                .content(subCategoryPage.getContent().stream()
                        .map(sc -> new AdminSubCategoryResponseDto(
                                sc.getId(), sc.getName(), sc.getIsActive()))
                        .toList())
                .page(subCategoryPage.getNumber())
                .size(subCategoryPage.getSize())
                .totalElements(subCategoryPage.getTotalElements())
                .totalPages(subCategoryPage.getTotalPages())
                .last(subCategoryPage.isLast())
                .build();
    }
    public PageResponseDto<AminAttributeResponseDto> getAttributes(int page, int size) {

        Page<AttributeConfig> attrPage = attributeRepo.findAll(
                PageRequest.of(page, size, Sort.by("id").descending()));

        return PageResponseDto.<AminAttributeResponseDto>builder()
                .content(attrPage.getContent().stream()
                        .map(a -> new AminAttributeResponseDto(
                                a.getId(), a.getName(), a.getFilterable()))
                        .toList())
                .page(attrPage.getNumber())
                .size(attrPage.getSize())
                .totalElements(attrPage.getTotalElements())
                .totalPages(attrPage.getTotalPages())
                .last(attrPage.isLast())
                .build();
    }
    public PageResponseDto<AttributeOptionResponseDto> getOptionsByAttribute(Long attributeId, int page, int size) {

        Page<AttributeOption> optionPage = attributeOptionRepo.findByAttributeId(
                attributeId, PageRequest.of(page, size, Sort.by("sortOrder").ascending()));

        return PageResponseDto.<AttributeOptionResponseDto>builder()
                .content(optionPage.getContent().stream()
                        .map(o -> new AttributeOptionResponseDto(
                                o.getId(), o.getValue(), o.getActive()))
                        .toList())
                .page(optionPage.getNumber())
                .size(optionPage.getSize())
                .totalElements(optionPage.getTotalElements())
                .totalPages(optionPage.getTotalPages())
                .last(optionPage.isLast())
                .build();
    }

}
