package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.project.backend.entity.Location;
import com.project.backend.requestDto.LocationRequestDto;
import com.project.backend.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Locations")
public class AdminLocationController {

    private final LocationService locationService;

    @Operation(summary = "Add new location (Admin)")
    @PostMapping
    public ResponseEntity<Location> createLocation(
            @Validated @RequestBody LocationRequestDto dto) {

        return ResponseEntity.ok(
                locationService.createLocation(dto)
        );
    }
    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id,
            @Validated @RequestBody LocationRequestDto dto) {

        return ResponseEntity.ok(locationService.updateLocation(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disableLocation(@PathVariable Long id) {
        locationService.disableLocation(id);
        return ResponseEntity.noContent().build();
    }

}
