package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.entity.ProductInventory;
import com.project.backend.requestDto.InventoryRequestDto;
import com.project.backend.service.InventoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Inventory")
public class AdminInventoryController {

	private final InventoryService inventoryService;

	@PostMapping
	public ResponseEntity<Void> upsertInventory(@Validated @RequestBody InventoryRequestDto dto) {

		inventoryService.upsertInventory(dto);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/product/{productId}")
	public ResponseEntity<List<ProductInventory>> getInventoryByProduct(@PathVariable Long productId) {

		return ResponseEntity.ok(inventoryService.getInventoryByProduct(productId));
	}

	@GetMapping("/location/{locationId}")
	public ResponseEntity<List<ProductInventory>> getInventoryByLocation(@PathVariable Long locationId) {

		return ResponseEntity.ok(inventoryService.getInventoryByLocation(locationId));
	}

	@GetMapping("/low-stock")
	public ResponseEntity<List<ProductInventory>> getLowStock(@RequestParam(defaultValue = "5") int threshold) {

		return ResponseEntity.ok(inventoryService.getLowStock(threshold));
	}

	@PostMapping("/bulk")
	public ResponseEntity<Void> bulkUpdate(@Validated @RequestBody List<InventoryRequestDto> dtos) {

		inventoryService.bulkUpsert(dtos);
		return ResponseEntity.ok().build();
	}

}
