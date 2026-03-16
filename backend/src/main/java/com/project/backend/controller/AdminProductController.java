package com.project.backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.requestDto.ProductRequestDto;
import com.project.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Products")
public class AdminProductController {

	private final ProductService productService;

	@Operation(summary = "Add product", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductResponseDto> addProduct(@RequestPart("product") @Valid ProductRequestDto dto,
			@RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {

		log.info("Creating product: {}", dto.getName());
		ProductResponseDto response = productService.create(dto, imageFiles);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	
	@Operation(summary = "Update product", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/{id}")
	public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id,
			@RequestBody @Valid ProductRequestDto dto) {

		log.info("Updating product with id: {}", id);
		ProductResponseDto response = productService.update(id, dto);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Soft delete product", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDeleteProduct(@PathVariable Long id) {
		log.info("Soft deleting product with id: {}", id);
		productService.softDeleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Activate product", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/{id}/activate")
	public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
		log.info("Activating product with id: {}", id);
		productService.activateProduct(id);
		return ResponseEntity.ok().build();
	}

//	/**
//	 * GET ALL products (admin view - includes inactive)
//	 */
//	@Operation(summary = "Get all products", security = { @SecurityRequirement(name = "Bearer Authentication") })
//	@GetMapping("/all")
//	public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
//		log.info("Fetching all products");
//		return ResponseEntity.ok(productService.getAll());
//	}


	@Operation(summary = "List products with pagination", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping
	public ResponseEntity<Page<ProductResponseDto>> listProducts(@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) String status, Pageable pageable) {

		log.info("Listing products - categoryId: {}, status: {}, page: {}", categoryId, status,
				pageable.getPageNumber());

		Page<ProductResponseDto> products = productService.listProducts(categoryId, status, pageable);
		return ResponseEntity.ok(products);
	}

	/**
	 * GET product by ID
	 */
	@Operation(summary = "Get product by ID", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
		log.info("Fetching product with id: {}", id);
		ProductResponseDto response = productService.getProduct(id);
		return ResponseEntity.ok(response);
	}

	// ============= VARIANT MANAGEMENT =============

	/**
	 * ADD variant to product
	 */
	@Operation(summary = "Add variant to product", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/{id}/variants")
	public ResponseEntity<ProductResponseDto> addVariant(@PathVariable Long id,
			@RequestBody @Valid ProductRequestDto.VariantRequest request) {

		log.info("Adding variant to product: {}", id);
		ProductResponseDto response = productService.addVariant(id, request);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * UPDATE variant
	 */
	@Operation(summary = "Update variant", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/variants/{variantId}")
	public ResponseEntity<ProductResponseDto> updateVariant(@PathVariable Long variantId,
			@RequestBody @Valid ProductRequestDto.VariantRequest request) {

		log.info("Updating variant: {}", variantId);
		ProductResponseDto response = productService.updateVariant(variantId, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * DEACTIVATE variant (soft delete)
	 */
	@Operation(summary = "Deactivate variant", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@DeleteMapping("/variants/{variantId}")
	public ResponseEntity<Void> deactivateVariant(@PathVariable Long variantId) {
		log.info("Deactivating variant: {}", variantId);
		productService.deactivateVariant(variantId);
		return ResponseEntity.noContent().build();
	}

	// ============= IMAGE MANAGEMENT =============

	/**
	 * UPLOAD images directly
	 */
	@Operation(summary = "Upload product images", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping(value = "/{id}/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductResponseDto> uploadImages(@PathVariable Long id,
			@RequestPart("images") List<MultipartFile> imageFiles) {

		log.info("Uploading {} images to product: {}", imageFiles.size(), id);
		ProductResponseDto response = productService.uploadImages(id, imageFiles);
		return ResponseEntity.ok(response);
	}

	/**
	 * DELETE image
	 */
	@Operation(summary = "Delete image", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@DeleteMapping("/images/{imageId}")
	public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
		log.info("Deleting image: {}", imageId);
		productService.deleteImage(imageId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * SET primary image
	 */
	@Operation(summary = "Set primary image", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/images/{imageId}/set-primary")
	public ResponseEntity<ProductResponseDto> setPrimaryImage(@PathVariable Long imageId) {
		log.info("Setting primary image: {}", imageId);
		ProductResponseDto response = productService.setPrimaryImage(imageId);
		return ResponseEntity.ok(response);
	}


}