package com.project.backend.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.requestDto.WarehouseDto;
import com.project.backend.service.WarehouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/warehouses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Warehouses")
public class WarehouseController {

	private final WarehouseService warehouseService;

	@Operation(summary = "Create warehouse", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping
	public ResponseEntity<WarehouseDto.Response> createWarehouse(@Valid @RequestBody WarehouseDto.Request request) {
		return new ResponseEntity<>(warehouseService.createWarehouse(request), HttpStatus.CREATED);
	}

	@Operation(summary = "Update warehouse", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/{id}")
	public ResponseEntity<WarehouseDto.Response> updateWarehouse(@PathVariable Long id,
			@Valid @RequestBody WarehouseDto.Request request) {
		return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
	}

	@Operation(summary = "Delete warehouse", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
		warehouseService.deleteWarehouse(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Deactivate warehouse", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<Void> deactivateWarehouse(@PathVariable Long id) {
		warehouseService.deactivateWarehouse(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Set as default warehouse", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@PatchMapping("/{id}/set-default")
	public ResponseEntity<Void> setDefaultWarehouse(@PathVariable Long id) {
		warehouseService.setDefaultWarehouse(id);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get all warehouses", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping
	public ResponseEntity<List<WarehouseDto.Response>> getAllWarehouses() {
		return ResponseEntity.ok(warehouseService.getAllActiveWarehouses());
	}

	@Operation(summary = "Get warehouses paginated", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/paginated")
	public ResponseEntity<Page<WarehouseDto.Response>> getWarehousesPaginated(Pageable pageable) {
		return ResponseEntity.ok(warehouseService.getActiveWarehousesPaginated(pageable));
	}

	@Operation(summary = "Get warehouse by ID", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/{id}")
	public ResponseEntity<WarehouseDto.Response> getWarehouseById(@PathVariable Long id) {
		return ResponseEntity.ok(warehouseService.getWarehouseById(id));
	}

	@Operation(summary = "Get warehouse inventory summary", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/{id}/inventory-summary")
	public ResponseEntity<WarehouseDto.InventorySummary> getInventorySummary(@PathVariable Long id) {
		return ResponseEntity.ok(warehouseService.getWarehouseInventorySummary(id));
	}
}