package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.OrderStatus;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.UpdateOrderStatusDto;
import com.project.backend.service.AdminOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "Search & filter orders", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping
    public ResponseEntity<PageResponseDto<OrderResponseDto>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                adminOrderService.searchOrders(status, userId, orderId, email, from, to, page, size)
        );
    }

    @Operation(summary = "Get order by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
    }

    @Operation(summary = "Update order status", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusDto dto) {

        return ResponseEntity.ok(adminOrderService.updateStatus(orderId, dto.getStatus()));
    }

    @Operation(summary = "Cancel order", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        adminOrderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
