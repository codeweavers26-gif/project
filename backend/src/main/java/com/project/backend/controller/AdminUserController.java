package com.project.backend.controller;

import com.project.backend.ResponseDto.UserResponseDto;
import com.project.backend.ResponseDto.UserSummaryDto;
import com.project.backend.service.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin APIs for managing and analyzing users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "Get all users",
            description = "Fetch paginated users with optional search and date filters",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getUsers(
            @Parameter(description = "Search by name or email") @RequestParam(required = false) String search,
            @Parameter(description = "Filter users created after date (yyyy-MM-dd)") @RequestParam(required = false) String fromDate,
            @Parameter(description = "Filter users created before date (yyyy-MM-dd)") @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(adminUserService.getUsers(search, fromDate, toDate, page, size));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Fetch detailed profile of a user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @Operation(
            summary = "User statistics",
            description = "Get total users and recent registrations",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/summary")
    public ResponseEntity<UserSummaryDto> getUserSummary() {
        return ResponseEntity.ok(adminUserService.getUserSummary());
    }
}
