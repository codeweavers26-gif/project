package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminCategoryResponseDto;
import com.project.backend.ResponseDto.AdminSectionResponseDto;
import com.project.backend.ResponseDto.AdminSubCategoryResponseDto;
import com.project.backend.ResponseDto.AminAttributeResponseDto;
import com.project.backend.ResponseDto.AttributeOptionResponseDto;
import com.project.backend.ResponseDto.AttributeResponseDto;
import com.project.backend.ResponseDto.CategoryResponseDto;
import com.project.backend.ResponseDto.SectionResponseDto;
import com.project.backend.ResponseDto.SubCategoryResponseDto;
import com.project.backend.entity.AttributeConfig;
import com.project.backend.entity.AttributeOption;
import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.entity.SubCategory;
import com.project.backend.entity.SubCategoryAttribute;
import com.project.backend.requestDto.AttributeOptionRequestDto;
import com.project.backend.requestDto.AttributeRequestDto;
import com.project.backend.requestDto.CategoryRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.SectionRequestDto;
import com.project.backend.requestDto.SubCategoryRequestDto;
import com.project.backend.service.AdminCatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Catalog Management")
public class AdminCatalogController {

    private final AdminCatalogService service;

    @Operation(summary = "Create section", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/sections")
    public ResponseEntity<Section> createSection(@RequestBody SectionRequestDto s) {
        return ResponseEntity.ok(service.createSection(s));
    }

    @Operation(summary = "Create category", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequestDto c) {
        return ResponseEntity.ok(service.createCategory(c));
    }

    @Operation(summary = "Create subcategory", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/subcategories")
    public ResponseEntity<SubCategory> createSubCategory(@RequestBody SubCategoryRequestDto sc) {
        return ResponseEntity.ok(service.createSubCategory(sc));
    }

    @Operation(summary = "Create attribute", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/attributes")
    public ResponseEntity<AttributeConfig> createAttribute(@RequestBody AttributeRequestDto a) {
        return ResponseEntity.ok(service.createAttribute(a));
    }

    @Operation(summary = "Add attribute option", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/attributes/{attributeId}/options")
    public ResponseEntity<AttributeOption> addOption(
            @PathVariable Long attributeId,
            @RequestBody AttributeOptionRequestDto option) {
        return ResponseEntity.ok(service.addAttributeOption(attributeId, option));
    }

    @Operation(summary = "Assign attribute to subcategory", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/subcategories/{subCategoryId}/attributes/{attributeId}")
    public ResponseEntity<SubCategoryAttribute> assignAttribute(
            @PathVariable Long subCategoryId,
            @PathVariable Long attributeId) {
        return ResponseEntity.ok(service.assignAttributeToSubCategory(subCategoryId, attributeId));
    }
    
    
    
    @Operation(summary = "Get all sections (paginated)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/sections")
    public ResponseEntity<PageResponseDto<AdminSectionResponseDto>> getSections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getSections(page, size));
    }
    @Operation(summary = "Get categories by section", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/sections/{sectionId}/categories")
    public ResponseEntity<PageResponseDto<AdminCategoryResponseDto>> getCategories(
            @PathVariable Long sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getCategoriesBySection(sectionId, page, size));
    }
    @Operation(summary = "Get subcategories by category", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/categories/{categoryId}/subcategories")
    public ResponseEntity<PageResponseDto<AdminSubCategoryResponseDto>> getSubCategories(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getSubCategoriesByCategory(categoryId, page, size));
    }
    @Operation(summary = "Get all attributes", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/attributes")
    public ResponseEntity<PageResponseDto<AminAttributeResponseDto>> getAttributes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getAttributes(page, size));
    }
    @Operation(summary = "Get attribute options", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/attributes/{attributeId}/options")
    public ResponseEntity<PageResponseDto<AttributeOptionResponseDto>> getOptions(
            @PathVariable Long attributeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(service.getOptionsByAttribute(attributeId, page, size));
    }

}
