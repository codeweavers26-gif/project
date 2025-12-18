package com.project.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderStatus;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.OrderRepository;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

	private final OrderRepository orderRepository;

	// GET ORDER BY ID
	public Order getOrderById(Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
	}

	// UPDATE ORDER STATUS
	@Transactional
	public Order updateStatus(Long orderId, OrderStatus status) {

		Order order = getOrderById(orderId);

		// Basic validation
		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
			throw new RuntimeException("Order status cannot be changed");
		}

		order.setStatus(status);
		return orderRepository.save(order);
	}

	public PageResponseDto<OrderResponseDto> getAllOrders(int page, int size) {

	    PageRequest pageable = PageRequest.of(
	            page,
	            size,
	            Sort.by("createdAt").descending()
	    );

	    Page<Order> orders = orderRepository.findAll(pageable);

	    return PageResponseDto.<OrderResponseDto>builder()
	            .content(
	                    orders.getContent()
	                            .stream()
	                            .map(OrderMapper::toDto)
	                            .toList()
	            )
	            .page(orders.getNumber())
	            .size(orders.getSize())
	            .totalElements(orders.getTotalElements())
	            .totalPages(orders.getTotalPages())
	            .last(orders.isLast())
	            .build();
	}

}
