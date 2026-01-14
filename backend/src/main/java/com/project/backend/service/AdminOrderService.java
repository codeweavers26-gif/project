package com.project.backend.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;

	// 🔹 Get order by ID
	public OrderResponseDto getOrderById(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		return OrderMapper.toDto(order);
	}

	// 🔹 Get all orders (with optional status filter)
	public PageResponseDto<OrderResponseDto> getAllOrders(OrderStatus status, int page, int size) {

		PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<Order> orders = (status == null) ? orderRepository.findAll(pageable)
				: orderRepository.findByStatus(status, pageable);

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

	// 🔹 Update order status
	@Transactional
	public OrderResponseDto updateStatus(Long orderId, OrderStatus newStatus) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		validateStatusTransition(order.getStatus(), newStatus);

		order.setStatus(newStatus);
		return OrderMapper.toDto(orderRepository.save(order));
	}

	// 🔹 Cancel order
	@Transactional
	public void cancelOrder(Long orderId) {

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		if (order.getStatus() == OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivered order cannot be cancelled");
		}

		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	}

	// 🔹 Order status validation (industry standard)
	private void validateStatusTransition(OrderStatus current, OrderStatus next) {

		if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Final order status cannot be changed");
		}

		if (current == OrderStatus.SHIPPED && next == OrderStatus.PLACED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status transition");
		}
	}

	// 🔹 Get orders by user
	public PageResponseDto<OrderResponseDto> getOrdersByUser(Long userId, OrderStatus status, int page, int size) {

		// Validate user exists
		userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<Order> orders = (status == null) ? orderRepository.findByUserId(userId, pageable)
				: orderRepository.findByUserIdAndStatus(userId, status, pageable);

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

	public PageResponseDto<OrderResponseDto> getOrdersByDate(String from, String to, int page, int size) {

		LocalDate fromDate = LocalDate.parse(from);
		LocalDate toDate = LocalDate.parse(to);

		PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<Order> orders = orderRepository.findByCreatedAtBetween(fromDate.atStartOfDay(),
				toDate.plusDays(1).atStartOfDay(), pageable);

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

	public PageResponseDto<OrderResponseDto> searchOrders(Long orderId, String email, int page, int size) {

		if (orderId == null && (email == null || email.isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide orderId or email");
		}

		PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<Order> orders;

		if (orderId != null) {
			orders = orderRepository.findById(orderId, pageable);
		} else {
			orders = orderRepository.findByUserEmailContainingIgnoreCase(email, pageable);
		}

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

}
