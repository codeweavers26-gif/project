package com.project.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.UserResponseDto;
import com.project.backend.ResponseDto.UserSummaryDto;
import com.project.backend.requestDto.OrderFilter;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.AdminUserService;
import com.project.backend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin APIs for managing and analyzing users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final OrderService orderService; 

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
    @Operation(
            summary = "Get user orders with filters",
            description = "Fetch orders for a specific user with multiple filter options",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/{userId}/orders")
    public ResponseEntity<PageResponseDto<OrderResponseDto>> getUserOrders(
            @PathVariable Long userId,
            
            // Order filters
            @Parameter(description = "Filter by order status") 
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by payment status") 
            @RequestParam(required = false) String paymentStatus,
            
            @Parameter(description = "Filter by payment method") 
            @RequestParam(required = false) String paymentMethod,
            
            @Parameter(description = "Filter by minimum amount") 
            @RequestParam(required = false) Double minAmount,
            
            @Parameter(description = "Filter by maximum amount") 
            @RequestParam(required = false) Double maxAmount,
            
            // Date filters
            @Parameter(description = "Orders from date (yyyy-MM-dd)") 
            @RequestParam(required = false) String fromDate,
            
            @Parameter(description = "Orders to date (yyyy-MM-dd)") 
            @RequestParam(required = false) String toDate,
            
            // Search
            @Parameter(description = "Search in order ID or items") 
            @RequestParam(required = false) String search,
            
            // Sorting
            @Parameter(description = "Sort by field (createdAt, totalAmount, status)") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (asc or desc)") 
            @RequestParam(defaultValue = "desc") String sortDirection,
            
            // Pagination
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        OrderFilter filter = OrderFilter.builder()
                .userId(userId)
                .status(status)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .fromDate(fromDate)
                .toDate(toDate)
                .search(search)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        return ResponseEntity.ok(orderService.getUserOrdersWithFilters(filter, page, size));
    }
    
}
