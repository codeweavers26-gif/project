package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.CheckoutResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.ReorderResponseDto;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

	private final OrderService orderService;
	private final UserRepository userRepository;

	private User getCurrentUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Operation(summary = "Checkout (login required)", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/checkout")
	public ResponseEntity<CheckoutResponseDto> checkout(Authentication auth,
			@Valid @RequestBody CheckoutRequestDto request) {

		User user = getCurrentUser(auth);
		CheckoutResponseDto order = orderService.checkout(user, request);
		return ResponseEntity.ok(order);
	}

	@Operation(summary = "Get logged-in user's orders", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/my-orders")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> myOrders(Authentication auth,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		User user = getCurrentUser(auth);

		return ResponseEntity.ok(orderService.loggedUserLogin(user, page, size));

	}

	@Operation(summary = "cancel order", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication auth) {

		User user = getCurrentUser(auth);
		orderService.cancelOrder(orderId, user);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{orderId}")
	@Operation(summary = "Get order details", security = { @SecurityRequirement(name = "Bearer Authentication") })
	public ResponseEntity<OrderResponseDto> getOrder(Authentication auth, @PathVariable Long orderId) {

		User user = getCurrentUser(auth);
		return ResponseEntity.ok(orderService.getOrderById(orderId, user));
	}

	@PostMapping("/{orderId}/reorder")
	@Operation(summary = "Reorder previous order", security = { @SecurityRequirement(name = "Bearer Authentication") })
	public ResponseEntity<Void> reorder(Authentication auth, @PathVariable Long orderId) {

		orderService.reorder(getCurrentUser(auth), orderId);
		return ResponseEntity.ok().build();
	}

//	@PostMapping("/items/{orderItemId}/return")
//	@Operation(summary = "Request return for order item", security = {
//			@SecurityRequirement(name = "Bearer Authentication") })
//	public ResponseEntity<Void> requestReturn(Authentication auth, @PathVariable Long orderItemId,
//			@RequestBody ReturnRequestDto dto) {
//
//		orderReturnService.requestReturn(getCurrentUser(auth), orderItemId, dto);
//		return ResponseEntity.ok().build();
//	}
//
//	@GetMapping("/returns")
//	@Operation(summary = "Get my returns", security = { @SecurityRequirement(name = "Bearer Authentication") })
//	public ResponseEntity<PageResponseDto<ReturnResponseDto>> myReturns(Authentication auth,
//			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
//
//		return ResponseEntity.ok(orderReturnService.getUserReturns(getCurrentUser(auth).getId(), page, size));
//	}

	
}
