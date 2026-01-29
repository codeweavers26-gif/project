package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.requestDto.ProductRequestDto;
import com.project.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Products")
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "Add product", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PostMapping
    public ResponseEntity<ProductResponseDto> addProduct(
    		@Valid  @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(productService.create(dto));
    }

    @Operation(summary = "Update product", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @Operation(summary = "Deactivate product")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get product", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== AUTH DEBUG ===");
        System.out.println("Principal: " + auth.getPrincipal());
        System.out.println("Authorities: " + auth.getAuthorities());
        System.out.println("Credentials: " + auth.getCredentials());
        System.out.println("==================");
        return ResponseEntity.ok(productService.getAll());
    }
}
