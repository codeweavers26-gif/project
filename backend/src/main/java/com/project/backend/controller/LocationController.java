package com.project.backend.controller;

import com.project.backend.ResponseDto.LocationResponseDto;
import com.project.backend.entity.Location;
import com.project.backend.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Customer - Locations", description = "Customer APIs for delivery serviceability")
public class LocationController {

    private final LocationService locationService;

    @Operation(
            summary = "Get all active serviceable locations",
            description = "Returns list of locations where delivery is available"
    )
    @GetMapping
    public ResponseEntity<List<LocationResponseDto>> getActiveLocations() {
        return ResponseEntity.ok(locationService.getActiveLocations());
    }

    @Operation(
            summary = "Check delivery availability by pincode",
            description = "Customer can check if delivery & COD are available for a pincode"
    )
    @GetMapping("/serviceable")
    public ResponseEntity<LocationResponseDto> checkServiceability(@RequestParam String pincode) {

        Location location = locationService.findServiceableLocation(pincode);

        return ResponseEntity.ok(
                LocationResponseDto.builder()
                        .id(location.getId())
                        .name(location.getName())
                        .city(location.getCity())
                        .state(location.getState())
                        .pincode(location.getPincode())
                        .deliveryDays(location.getDeliveryDays())
                        .codAvailable(location.getCodAvailable())
                        .build()
        );
    }
}
