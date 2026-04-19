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

import com.project.backend.ResponseDto.AdminUserOrderResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.OrderStatus;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.UpdateOrderStatusDto;
import com.project.backend.service.AdminOrderService;
import com.project.backend.service.ShiprocketService;
import com.project.backend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	private final ShiprocketService shiprocketService;
	private final OrderService orderService;

	@Operation(summary = "Search & filter orders", security = @SecurityRequirement(name = "Bearer Authentication"))
	@GetMapping
	public ResponseEntity<PageResponseDto<OrderResponseDto>> searchOrders(
			@RequestParam(required = false) OrderStatus status, @RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long orderId, @RequestParam(required = false) String email,
			@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(adminOrderService.searchOrders(status, userId, orderId, email, from, to, page, size));
	}

	@Operation(summary = "Get order by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {
		return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
	}

	@Operation(summary = "Update order status", security = @SecurityRequirement(name = "Bearer Authentication"))
	@PutMapping("/{orderId}/status")
	public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable Long orderId,
			@RequestBody UpdateOrderStatusDto dto) {

		return ResponseEntity.ok(adminOrderService.updateStatus(orderId, dto.getStatus()));
	}

	@Operation(summary = "Cancel order", security = @SecurityRequirement(name = "Bearer Authentication"))
	@ApiResponses(value = {
		    @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
		    @ApiResponse(responseCode = "400", description = "Invalid request - Order cannot be cancelled (delivered, shipped, or already cancelled)"),
		    @ApiResponse(responseCode = "404", description = "Order not found"),
		    @ApiResponse(responseCode = "500", description = "Internal server error")
		})
	@PutMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
		adminOrderService.cancelOrder(orderId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/user/{userId}/orders")
	@Operation(summary = "Get all orders of a user", security = @SecurityRequirement(name = "Bearer Authentication"))
	public ResponseEntity<PageResponseDto<AdminUserOrderResponseDto>> getUserOrders(@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(adminOrderService.getOrdersOfUser(userId, page, size));
	}

	@Operation(summary = "Test Shiprocket auth + fetch pickup locations", security = @SecurityRequirement(name = "Bearer Authentication"))
	@GetMapping("/shiprocket/test")
	public ResponseEntity<java.util.Map<String, Object>> testShiprocket() {
		java.util.Map<String, Object> result = new java.util.HashMap<>();
		try {
			String token = shiprocketService.getValidToken();
			result.put("auth", "SUCCESS");
			result.put("tokenPreview", token.substring(0, Math.min(20, token.length())) + "...");

			org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
			headers.setBearerAuth(token);
			org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
			org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
			org.springframework.http.ResponseEntity<java.util.Map> res = rt.exchange(
				"https://apiv2.shiprocket.in/v1/external/settings/company/pickup",
				org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
			result.put("pickupLocations", res.getBody());
		} catch (Exception e) {
			result.put("error", e.getMessage());
			result.put("cause", e.getCause() != null ? e.getCause().getMessage() : null);
		}
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Manually trigger shipping for an order", security = @SecurityRequirement(name = "Bearer Authentication"))
	@org.springframework.web.bind.annotation.PostMapping("/{orderId}/trigger-shipping")
	public ResponseEntity<java.util.Map<String, Object>> triggerShipping(@PathVariable Long orderId) {
		java.util.Map<String, Object> result = new java.util.HashMap<>();
		try {
			orderService.triggerShippingAsync(orderId);
			result.put("status", "triggered");
			result.put("orderId", orderId);
		} catch (Exception e) {
			result.put("error", e.getMessage());
		}
		return ResponseEntity.ok(result);
	}
}
