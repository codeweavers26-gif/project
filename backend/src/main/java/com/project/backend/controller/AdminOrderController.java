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

	// 🔹 Get all orders (with optional status filter)
	@Operation(summary = "Get all orders", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping
	public ResponseEntity<PageResponseDto<OrderResponseDto>> getAllOrders(
			@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(adminOrderService.getAllOrders(status, page, size));
	}

	// 🔹 Get order by ID
	@Operation(summary = "Get order by ID", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {

		return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
	}

	// 🔹 Update order status
	@Operation(summary = "Update order status", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/{orderId}/status")
	public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long orderId,
			@RequestBody UpdateOrderStatusDto dto) {

		return ResponseEntity.ok(adminOrderService.updateStatus(orderId, dto.getStatus()));
	}

	// 🔹 Cancel order (admin power)
	@Operation(summary = "Cancel order", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@PutMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {

		adminOrderService.cancelOrder(orderId);
		return ResponseEntity.ok().build();
	}

	// 🔹 Get orders by user
	@Operation(summary = "Get orders by user", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/user/{userId}")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> getOrdersByUser(@PathVariable Long userId,
			@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(adminOrderService.getOrdersByUser(userId, status, page, size));
	}
	
	@Operation(summary = "Get orders by date range", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/by-date")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> getOrdersByDate(
	        @RequestParam String from,
	        @RequestParam String to,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size) {

	    return ResponseEntity.ok(
	            adminOrderService.getOrdersByDate(from, to, page, size)
	    );
	}
	
	@Operation(summary = "Search orders by orderId or user email", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/search")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> searchOrders(
	        @RequestParam(required = false) Long orderId,
	        @RequestParam(required = false) String email,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size) {

	    return ResponseEntity.ok(
	            adminOrderService.searchOrders(orderId, email, page, size)
	    );
	}


}
