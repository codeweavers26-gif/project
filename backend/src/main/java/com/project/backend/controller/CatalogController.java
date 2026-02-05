package com.project.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.entity.SubCategory;
import com.project.backend.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "User - Catalog APIs")
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Get all active sections")
    @GetMapping("/sections")
    public ResponseEntity<List<Section>> getSections() {
        return ResponseEntity.ok(catalogService.getActiveSections());
    }

    @Operation(summary = "Get categories by section")
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(@RequestParam Long sectionId) {
        return ResponseEntity.ok(catalogService.getCategoriesBySection(sectionId));
    }

    @Operation(summary = "Get subcategories by category")
    @GetMapping("/subcategories")
    public ResponseEntity<List<SubCategory>> getSubCategories(@RequestParam Long categoryId) {
        return ResponseEntity.ok(catalogService.getSubCategories(categoryId));
    }

    @Operation(summary = "Get filters for subcategory")
    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilters(@RequestParam Long subCategoryId) {
        return ResponseEntity.ok(catalogService.getFiltersForSubCategory(subCategoryId));
    }
}
