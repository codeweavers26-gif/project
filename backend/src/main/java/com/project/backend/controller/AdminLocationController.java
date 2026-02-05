package com.project.backend.controller;

import com.project.backend.entity.Location;
import com.project.backend.requestDto.LocationRequestDto;
import com.project.backend.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Locations", description = "Admin APIs for managing delivery locations")
public class AdminLocationController {

    private final LocationService locationService;

    @Operation(
            summary = "Create a new delivery location",
            description = "Admin can add a new serviceable location",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping
    public ResponseEntity<Location> createLocation(
            @Validated @RequestBody LocationRequestDto dto) {
        return ResponseEntity.ok(locationService.createLocation(dto));
    }

    @Operation(
            summary = "Update an existing location",
            description = "Admin updates delivery configuration of a location",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id,
            @Validated @RequestBody LocationRequestDto dto) {
        return ResponseEntity.ok(locationService.updateLocation(id, dto));
    }

    @Operation(
            summary = "Disable a location",
            description = "Marks a location as inactive so it is no longer serviceable",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disableLocation(@PathVariable Long id) {
        locationService.disableLocation(id);
        return ResponseEntity.noContent().build();
    }
}
