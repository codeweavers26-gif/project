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
import com.project.backend.entity.Order;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.UpdateOrderStatusDto;
import com.project.backend.service.AdminOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin - Orders")
public class AdminOrderController {

	private final AdminOrderService adminOrderService;

	@Operation(summary = "Get all orders")
	@GetMapping
	public ResponseEntity<PageResponseDto<OrderResponseDto>> getAllOrders(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size) {

	    return ResponseEntity.ok(
	            adminOrderService.getAllOrders(page, size)
	    );
	}

	@Operation(summary = "Get order by ID")
	@GetMapping("/{orderId}")
	public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
		return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
	}

	@Operation(summary = "Update order status")
	@PutMapping("/{orderId}/status")
	public ResponseEntity<Order> updateStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusDto dto) {

		return ResponseEntity.ok(adminOrderService.updateStatus(orderId, dto.getStatus()));
	}
}
