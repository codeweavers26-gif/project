package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminDashboardResponse;
import com.project.backend.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "APIs for admin analytics and system metrics")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @Operation(
        summary = "Get Admin Dashboard Metrics",
        description = "Returns overall platform statistics like sales, orders, users, inventory, and cart data.",
        security = @SecurityRequirement(name = "bearerAuth") // 🔐 JWT protected
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard metrics fetched successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN users can access this")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardMetrics());
    }
}
