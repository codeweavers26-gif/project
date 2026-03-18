package com.project.backend.controller;

import java.util.List;

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
import com.project.backend.ResponseDto.CategoryResponseDto;
import com.project.backend.entity.AttributeConfig;
import com.project.backend.entity.AttributeOption;
import com.project.backend.entity.Category;
import com.project.backend.entity.Section;
import com.project.backend.requestDto.AttributeOptionRequestDto;
import com.project.backend.requestDto.AttributeRequestDto;
import com.project.backend.requestDto.CategoryRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.SectionRequestDto;
import com.project.backend.service.AdminCatalogService;
import com.project.backend.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Catalog Management")
public class AdminCatalogController {
	private final CatalogService catalogService;

    @Operation(summary = "Get all active sections")
    @GetMapping("/sections")
    public ResponseEntity<List<Section>> getSections() {
        return ResponseEntity.ok(catalogService.getActiveSections());
    }

    @Operation(summary = "Get categories by section")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getCategories(@RequestParam Long sectionId) {
        return ResponseEntity.ok(catalogService.getCategoriesBySection(sectionId));
    }
}
