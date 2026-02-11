package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.InventoryResponseDto;
import com.project.backend.requestDto.InventoryAdjustRequestDto;
import com.project.backend.requestDto.InventoryRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Inventory")
public class AdminInventoryController {

	private final InventoryService inventoryService;

	/*
	 * ------------------------------------------------- CREATE / UPDATE
	 * -------------------------------------------------
	 */
	@PostMapping
	@Operation(summary = "Create or update inventory", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<Void> upsertInventory(@Validated @RequestBody InventoryRequestDto dto) {

		inventoryService.upsertInventory(dto);
		return ResponseEntity.ok().build();
	}

	/*
	 * ------------------------------------------------- SEARCH INVENTORY
	 * (PAGINATED) -------------------------------------------------
	 */
	@GetMapping
	@Operation(summary = "Search inventory (paginated)", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<PageResponseDto<InventoryResponseDto>> searchInventory(
			@RequestParam(required = false) Long productId, @RequestParam(required = false) Long locationId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(inventoryService.searchInventory(productId, locationId, page, size));
	}

	/*
	 * ------------------------------------------------- ADJUST STOCK
	 * -------------------------------------------------
	 */
	@PutMapping("/{inventoryId}/adjust")
	@Operation(summary = "Adjust stock (+ / -)", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<InventoryResponseDto> adjustStock(@PathVariable Long inventoryId,
			@RequestBody InventoryAdjustRequestDto dto) {

		return ResponseEntity.ok(inventoryService.adjustStock(inventoryId, dto.getDelta()));
	}

	/*
	 * ------------------------------------------------- LOW STOCK
	 * -------------------------------------------------
	 */
	@GetMapping("/low-stock")
	@Operation(summary = "Low stock items", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<PageResponseDto<InventoryResponseDto>> lowStock(
			@RequestParam(defaultValue = "5") int threshold, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(inventoryService.getLowStock(threshold, page, size));
	}

	/*
	 * ------------------------------------------------- OUT OF STOCK
	 * -------------------------------------------------
	 */
	@GetMapping("/out-of-stock")
	@Operation(summary = "Out of stock products", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<List<InventoryResponseDto>> outOfStock() {

		return ResponseEntity.ok(inventoryService.getOutOfStock());
	}

	/*
	 * ------------------------------------------------- BULK UPLOAD
	 * -------------------------------------------------
	 */
	@PostMapping("/bulk")
	@Operation(summary = "Bulk inventory upload", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<Void> bulkUpsert(@Validated @RequestBody List<InventoryRequestDto> dtos) {

		inventoryService.bulkUpsert(dtos);
		return ResponseEntity.ok().build();
	}
}
