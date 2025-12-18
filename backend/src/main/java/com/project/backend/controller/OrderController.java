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

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
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

	@Operation(summary = "Checkout (login required)")
	@PostMapping("/checkout")
	public ResponseEntity<Order> checkout(Authentication auth, @Valid @RequestBody CheckoutRequestDto request) {

		User user = getCurrentUser(auth);
		Order order = orderService.checkout(user, request);
		return ResponseEntity.ok(order);
	}

	@Operation(summary = "Get logged-in user's orders")
	@GetMapping("/my-orders")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> myOrders(Authentication auth,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		User user = getCurrentUser(auth);

		return ResponseEntity.ok(orderService.loggedUserLogin(user, page, size));

	}

	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication auth) {

		User user = getCurrentUser(auth);
		orderService.cancelOrder(orderId, user);
		return ResponseEntity.ok().build();
	}

}
