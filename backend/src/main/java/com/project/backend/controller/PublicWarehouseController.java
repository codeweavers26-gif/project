package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.requestDto.WarehouseDto;
import com.project.backend.service.WarehouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/warehouses")
@RequiredArgsConstructor
@Tag(name = "Public - Warehouses")
public class PublicWarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Get default warehouse")
    @GetMapping("/default")
    public ResponseEntity<WarehouseDto.Response> getDefaultWarehouse() {
        return ResponseEntity.ok(warehouseService.getDefaultWarehouse());
    }

    @Operation(summary = "Get warehouse by location")
    @GetMapping("/by-location/{locationId}")
    public ResponseEntity<WarehouseDto.Response> getWarehouseByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(warehouseService.getWarehouseByLocation(locationId));
    }

    @Operation(summary = "Get all active warehouses")
    @GetMapping
    public ResponseEntity<List<WarehouseDto.Response>> getAllActiveWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllActiveWarehouses());
    }
}