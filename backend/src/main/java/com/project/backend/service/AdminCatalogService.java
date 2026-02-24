package com.project.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminCatalogService {
//
//    private final SectionRepository sectionRepo;
//    private final CategoryRepository categoryRepo;
//    private final AttributeConfigRepository attributeRepo;
//    private final AttributeOptionRepository attributeOptionRepo;
//
//    // ============= SECTION OPERATIONS =============
//
//    public Section createSection(SectionRequestDto dto) {
//        // Check if section with same name exists
//        if (sectionRepo.findByName(dto.getName()).isPresent()) {
//            throw new BadRequestException("Section with name '" + dto.getName() + "' already exists");
//        }
//
//        Section section = Section.builder()
//                .name(dto.getName())
//                .imageUrl(dto.getImageUrl())
//                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                .build();
//
//        Section saved = sectionRepo.save(section);
//        log.info("Section created: {} (ID: {})", saved.getName(), saved.getId());
//        return saved;
//    }
//
//    public PageResponseDto<AdminSectionResponseDto> getSections(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
//        Page<Section> sections = sectionRepo.findAll(pageable);
//
//        List<AdminSectionResponseDto> content = sections.getContent().stream()
//                .map(this::mapToAdminSectionDto)
//                .collect(Collectors.toList());
//
//        return PageResponseDto.<AdminSectionResponseDto>builder()
//                .content(content)
//                .page(sections.getNumber())
//                .size(sections.getSize())
//                .totalElements(sections.getTotalElements())
//                .totalPages(sections.getTotalPages())
//                .last(sections.isLast())
//                .build();
//    }
//
//    // ============= CATEGORY OPERATIONS =============
//
//    public Category createCategory(CategoryRequestDto dto) {
//        Section section = sectionRepo.findById(dto.getSectionId())
//                .orElseThrow(() -> new NotFoundException("Section not found with ID: " + dto.getSectionId()));
//
//        // Check if category with same name exists in this section
//        if (categoryRepo.findByNameAndSectionId(dto.getName(), dto.getSectionId()).isPresent()) {
//            throw new BadRequestException("Category '" + dto.getName() + "' already exists in this section");
//        }
//
//        Category category = Category.builder()
//                .section(section)
//                .name(dto.getName())
//                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                .build();
//
//        Category saved = categoryRepo.save(category);
//        log.info("Category created: {} (ID: {}) under Section: {}", saved.getName(), saved.getId(), section.getName());
//        return saved;
//    }
//
//    public PageResponseDto<AdminCategoryResponseDto> getCategoriesBySection(Long sectionId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
//        Page<Category> categories = categoryRepo.findBySectionId(sectionId, pageable);
//
//        List<AdminCategoryResponseDto> content = categories.getContent().stream()
//                .map(this::mapToAdminCategoryDto)
//                .collect(Collectors.toList());
//
//        return PageResponseDto.<AdminCategoryResponseDto>builder()
//                .content(content)
//                .page(categories.getNumber())
//                .size(categories.getSize())
//                .totalElements(categories.getTotalElements())
//                .totalPages(categories.getTotalPages())
//                .last(categories.isLast())
//                .build();
//    }
//
//    // ============= SUBCATEGORY OPERATIONS =============
//
//    public SubCategory createSubCategory(SubCategoryRequestDto dto) {
//        Category category = categoryRepo.findById(dto.getCategoryId())
//                .orElseThrow(() -> new NotFoundException("Category not found with ID: " + dto.getCategoryId()));
//
//        // Check if subcategory with same name exists in this category
//        if (subCategoryRepo.findByNameAndCategoryId(dto.getName(), dto.getCategoryId()).isPresent()) {
//            throw new BadRequestException("SubCategory '" + dto.getName() + "' already exists in this category");
//        }
//
//        SubCategory subCategory = SubCategory.builder()
//                .category(category)
//                .name(dto.getName())
//                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                .build();
//
//        SubCategory saved = subCategoryRepo.save(subCategory);
//        log.info("SubCategory created: {} (ID: {}) under Category: {}", saved.getName(), saved.getId(), category.getName());
//        return saved;
//    }
//
//    public PageResponseDto<AdminSubCategoryResponseDto> getSubCategoriesByCategory(Long categoryId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
//        Page<SubCategory> subCategories = subCategoryRepo.findByCategoryId(categoryId, pageable);
//
//        List<AdminSubCategoryResponseDto> content = subCategories.getContent().stream()
//                .map(this::mapToAdminSubCategoryDto)
//                .collect(Collectors.toList());
//
//        return PageResponseDto.<AdminSubCategoryResponseDto>builder()
//                .content(content)
//                .page(subCategories.getNumber())
//                .size(subCategories.getSize())
//                .totalElements(subCategories.getTotalElements())
//                .totalPages(subCategories.getTotalPages())
//                .last(subCategories.isLast())
//                .build();
//    }
//
//    // ============= ATTRIBUTE OPERATIONS =============
//
//    public AttributeConfig createAttribute(AttributeRequestDto dto) {
//        // Check if attribute with same name exists
//        if (attributeRepo.findByName(dto.getName()).isPresent()) {
//            throw new BadRequestException("Attribute '" + dto.getName() + "' already exists");
//        }
//
//        AttributeConfig attribute = AttributeConfig.builder()
//                .name(dto.getName())
//                .displayName(dto.getDisplayName())
//                .type(dto.getType())
//                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false)
//                .isFilterable(dto.getIsFilterable() != null ? dto.getIsFilterable() : true)
//                .build();
//
//        AttributeConfig saved = attributeRepo.save(attribute);
//        log.info("Attribute created: {} (ID: {})", saved.getName(), saved.getId());
//        return saved;
//    }
//
//    public AttributeOption addAttributeOption(Long attributeId, AttributeOptionRequestDto dto) {
//        AttributeConfig attribute = attributeRepo.findById(attributeId)
//                .orElseThrow(() -> new NotFoundException("Attribute not found with ID: " + attributeId));
//
//        // Check if option with same value exists for this attribute
//        if (attributeOptionRepo.findByAttributeIdAndValue(attributeId, dto.getValue()).isPresent()) {
//            throw new BadRequestException("Option '" + dto.getValue() + "' already exists for this attribute");
//        }
//
//        AttributeOption option = AttributeOption.builder()
//                .attribute(attribute)
//                .value(dto.getValue())
//                .displayValue(dto.getDisplayValue() != null ? dto.getDisplayValue() : dto.getValue())
//                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
//                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                .build();
//
//        AttributeOption saved = attributeOptionRepo.save(option);
//        log.info("Option '{}' added to attribute: {}", saved.getValue(), attribute.getName());
//        return saved;
//    }
//
//    public SubCategoryAttribute assignAttributeToSubCategory(Long subCategoryId, Long attributeId) {
//        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
//                .orElseThrow(() -> new NotFoundException("SubCategory not found with ID: " + subCategoryId));
//
//        AttributeConfig attribute = attributeRepo.findById(attributeId)
//                .orElseThrow(() -> new NotFoundException("Attribute not found with ID: " + attributeId));
//
//        // Check if already assigned
//        if (subCategoryAttributeRepo.findBySubCategoryIdAndAttributeId(subCategoryId, attributeId).isPresent()) {
//            throw new BadRequestException("Attribute already assigned to this subcategory");
//        }
//
//        SubCategoryAttribute assignment = SubCategoryAttribute.builder()
//                .subCategory(subCategory)
//                .attribute(attribute)
//                .build();
//
//        SubCategoryAttribute saved = subCategoryAttributeRepo.save(assignment);
//        log.info("Attribute '{}' assigned to SubCategory '{}'", attribute.getName(), subCategory.getName());
//        return saved;
//    }
//
//    public PageResponseDto<AminAttributeResponseDto> getAttributes(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
//        Page<AttributeConfig> attributes = attributeRepo.findAll(pageable);
//
//        List<AminAttributeResponseDto> content = attributes.getContent().stream()
//                .map(this::mapToAdminAttributeDto)
//                .collect(Collectors.toList());
//
//        return PageResponseDto.<AminAttributeResponseDto>builder()
//                .content(content)
//                .page(attributes.getNumber())
//                .size(attributes.getSize())
//                .totalElements(attributes.getTotalElements())
//                .totalPages(attributes.getTotalPages())
//                .last(attributes.isLast())
//                .build();
//    }
//
//    public PageResponseDto<AttributeOptionResponseDto> getOptionsByAttribute(Long attributeId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("sortOrder").ascending());
//        Page<AttributeOption> options = attributeOptionRepo.findByAttributeId(attributeId, pageable);
//
//        List<AttributeOptionResponseDto> content = options.getContent().stream()
//                .map(this::mapToAttributeOptionDto)
//                .collect(Collectors.toList());
//
//        return PageResponseDto.<AttributeOptionResponseDto>builder()
//                .content(content)
//                .page(options.getNumber())
//                .size(options.getSize())
//                .totalElements(options.getTotalElements())
//                .totalPages(options.getTotalPages())
//                .last(options.isLast())
//                .build();
//    }
//
//    // ============= UPDATE/DELETE METHODS =============
//
//    @Transactional
//    public Section updateSection(Long id, SectionRequestDto dto) {
//        Section section = sectionRepo.findById(id)
//                .orElseThrow(() -> new NotFoundException("Section not found with ID: " + id));
//
//        if (dto.getName() != null && !dto.getName().equals(section.getName())) {
//            if (sectionRepo.findByName(dto.getName()).isPresent()) {
//                throw new BadRequestException("Section with name '" + dto.getName() + "' already exists");
//            }
//            section.setName(dto.getName());
//        }
//
//        if (dto.getImageUrl() != null) {
//            section.setImageUrl(dto.getImageUrl());
//        }
//
//        if (dto.getIsActive() != null) {
//            section.setIsActive(dto.getIsActive());
//        }
//
//        Section updated = sectionRepo.save(section);
//        log.info("Section updated: {} (ID: {})", updated.getName(), updated.getId());
//        return updated;
//    }
//
//    @Transactional
//    public void deleteSection(Long id) {
//        Section section = sectionRepo.findById(id)
//                .orElseThrow(() -> new NotFoundException("Section not found with ID: " + id));
//
//        // Check if section has categories
//        long categoryCount = categoryRepo.countBySectionId(id);
//        if (categoryCount > 0) {
//            throw new BadRequestException("Cannot delete section with existing categories. Deactivate it instead.");
//        }
//
//        sectionRepo.delete(section);
//        log.info("Section deleted: ID: {}", id);
//    }
//
//    // ============= MAPPERS =============
//
//    private AdminSectionResponseDto mapToAdminSectionDto(Section section) {
//        long categoryCount = categoryRepo.countBySectionId(section.getId());
//
//        return AdminSectionResponseDto.builder()
//                .id(section.getId())
//                .name(section.getName())
//                .imageUrl(section.getImageUrl())
//                .isActive(section.getIsActive())
//                .categoryCount((int) categoryCount)
//                .build();
//    }
//
//    private AdminCategoryResponseDto mapToAdminCategoryDto(Category category) {
//        long subCategoryCount = subCategoryRepo.countByCategoryId(category.getId());
//
//        return AdminCategoryResponseDto.builder()
//                .id(category.getId())
//                .name(category.getName())
//                .sectionId(category.getSection().getId())
//                .sectionName(category.getSection().getName())
//                .isActive(category.getIsActive())
//                .subCategoryCount((int) subCategoryCount)
//                .build();
//    }
//
//    private AdminSubCategoryResponseDto mapToAdminSubCategoryDto(SubCategory subCategory) {
//        long productCount = 0; // You'll need to add this query
//        long attributeCount = subCategoryAttributeRepo.countBySubCategoryId(subCategory.getId());
//
//        return AdminSubCategoryResponseDto.builder()
//                .id(subCategory.getId())
//                .name(subCategory.getName())
//                .categoryId(subCategory.getCategory().getId())
//                .categoryName(subCategory.getCategory().getName())
//                .sectionId(subCategory.getCategory().getSection().getId())
//                .sectionName(subCategory.getCategory().getSection().getName())
//                .isActive(subCategory.getIsActive())
//                .productCount((int) productCount)
//                .attributeCount((int) attributeCount)
//                .build();
//    }
//
//    private AminAttributeResponseDto mapToAdminAttributeDto(AttributeConfig attribute) {
//        long optionCount = attributeOptionRepo.countByAttributeId(attribute.getId());
//
//        return AminAttributeResponseDto.builder()
//                .id(attribute.getId())
//                .name(attribute.getName())
//                .displayName(attribute.getDisplayName())
//                .type(attribute.getType())
//                .isRequired(attribute.getIsRequired())
//                .isFilterable(attribute.getIsFilterable())
//                .optionCount((int) optionCount)
//                .build();
//    }
//
//    private AttributeOptionResponseDto mapToAttributeOptionDto(AttributeOption option) {
//        return AttributeOptionResponseDto.builder()
//                .id(option.getId())
//                .attributeId(option.getAttribute().getId())
//                .attributeName(option.getAttribute().getName())
//                .value(option.getValue())
//                .displayValue(option.getDisplayValue())
//                .sortOrder(option.getSortOrder())
//                .isActive(option.getIsActive())
//                .build();
//    }
}