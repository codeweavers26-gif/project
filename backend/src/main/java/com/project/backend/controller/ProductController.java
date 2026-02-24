package com.project.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.requestDto.BreadcrumbDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductFilterDto;
import com.project.backend.service.ProductFilterService;
import com.project.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
//
//GET    /products (with filters, pagination)
//GET    /products/{slug}
//GET    /products/{id}/variants (availability)
//GET    /categories
//GET    /categories/{slug}/products

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products (Customer)")
public class ProductController {

	private final ProductService productService;

	@GetMapping
	@Operation(summary = "Get active products with pagination and filters")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getProducts(
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Double minPrice,
			@RequestParam(required = false) Double maxPrice, @RequestParam(required = false) String size,
			@RequestParam(required = false) String color, @RequestParam(required = false) String brand,
			@RequestParam(required = false) String sortBy, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int limit) {

		ProductFilterDto filter = ProductFilterDto.builder().categoryId(categoryId).minPrice(minPrice)
				.maxPrice(maxPrice).limit(limit).color(color).brand(brand).sortBy(sortBy).page(page).size(size).build();

		return ResponseEntity.ok(productService.getActiveProducts(filter));
	}

	// GET PRODUCT BY ID
	@Operation(summary = "Get product details by ID", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.getActiveProductById(id));
	}

	/**
	 * Get products available at a location
	 */
	@Operation(summary = "Get products by location", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping(params = { "locationId" })
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getProductsByLocation(@RequestParam Long locationId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(productService.getProductsByLocation(locationId, page, size));
	}

	private final ProductFilterService filterService;

	@GetMapping("/filter")
	@Operation(summary = "Filter products by hierarchy")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> filterProducts(@ModelAttribute ProductFilterDto filter) {
		return ResponseEntity.ok(filterService.filterProducts(filter));
	}

	@GetMapping("/filter/options")
	@Operation(summary = "Get filter options for current category")
	public ResponseEntity<Map<String, Object>> getFilterOptions(@RequestParam(required = false) Long sectionId,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long subCategoryId) {
		return ResponseEntity.ok(filterService.getFilterOptions(sectionId, categoryId, subCategoryId));
	}

	@GetMapping("/breadcrumb")
	@Operation(summary = "Get breadcrumb navigation")
	public ResponseEntity<List<BreadcrumbDto>> getBreadcrumb(@RequestParam(required = false) Long sectionId,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long subCategoryId) {
		return ResponseEntity.ok(filterService.getBreadcrumb(sectionId, categoryId, subCategoryId));
	}

	@GetMapping("/featured")
	@Operation(summary = "Get featured products")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getFeaturedProducts(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(productService.getFeaturedProducts(page, size));
	}

	@GetMapping("/new-arrivals")
	@Operation(summary = "Get new arrivals")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getNewArrivals(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(productService.getNewArrivals(page, size));
	}

	@GetMapping("/best-sellers")
	@Operation(summary = "Get best selling products")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getBestSellers(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(productService.getBestSellers(page, size));
	}

	@GetMapping("/{productId}/related")
	@Operation(summary = "Get related products")
	public ResponseEntity<PageResponseDto<ProductResponseDto>> getRelatedProducts(@PathVariable Long productId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size) {

		return ResponseEntity.ok(productService.getRelatedProducts(productId, page, size));
	}
}
