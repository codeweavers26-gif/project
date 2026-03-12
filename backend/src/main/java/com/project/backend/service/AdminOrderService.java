package com.project.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.project.backend.ResponseDto.AdminUserOrderResponseDto;
import com.project.backend.ResponseDto.OrderAddressDto;
import com.project.backend.ResponseDto.OrderItemAdminDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
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

	public OrderResponseDto getOrderById(Long orderId) {
		return OrderMapper.toDto(getOrder(orderId));
	}

	public PageResponseDto<OrderResponseDto> searchOrders(OrderStatus status, Long userId, Long orderId, String email,
	        String from, String to, int page, int size) {

	    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

	    Instant fromDate = from != null ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
	    Instant toDate = to != null ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
	            : null;

	    Page<Order> orders = orderRepository.searchOrders(status, userId, orderId, email, fromDate, toDate, pageable);

	    return PageResponseDto.<OrderResponseDto>builder()
	            .content(orders.getContent().stream()
	                    .map(order -> {
	                        OrderResponseDto dto = OrderMapper.toDto(order);
	                        
	                        return dto;
	                    })
	                    .collect(Collectors.toList()))
	            .page(orders.getNumber())
	            .size(orders.getSize())
	            .totalElements(orders.getTotalElements())
	            .totalPages(orders.getTotalPages())
	            .last(orders.isLast())
	            .build();
	}
	@Transactional
	public OrderResponseDto updateStatus(Long orderId, OrderStatus newStatus) {

		Order order = getOrder(orderId);
		validateStatusTransition(order.getStatus(), newStatus);

		order.setStatus(newStatus);
		return OrderMapper.toDto(orderRepository.save(order));
	}

	@Transactional
	public void cancelOrder(Long orderId) {
		Order order = getOrder(orderId);

		if (order.getStatus() == OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivered order cannot be cancelled");
		}

		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	}

	private Order getOrder(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
	}

	private void validateStatusTransition(OrderStatus current, OrderStatus next) {

	    if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
	            "Cannot change status of a " + current + " order");
	    }
	    
	    Map<OrderStatus, Set<OrderStatus>> validTransitions = Map.of(
	        OrderStatus.PENDING, Set.of(OrderStatus.PAID, OrderStatus.PLACED, OrderStatus.CANCELLED),
	        OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID, OrderStatus.PLACED, OrderStatus.CANCELLED),
	        OrderStatus.PLACED, Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
	        OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
	        OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED),
	        OrderStatus.RETURN_REQUESTED, Set.of(OrderStatus.CANCELLED)
	    );
	    
	    Set<OrderStatus> allowedNext = validTransitions.get(current);
	    if (allowedNext == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
	            "Unknown order status: " + current);
	    }
	    
	    if (!allowedNext.contains(next)) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
	            "Invalid transition from " + current + " to " + next);
	    }
	}
	public PageResponseDto<AdminUserOrderResponseDto> getOrdersOfUser(Long userId, int page, int size) {

		Page<Order> orders = orderRepository.findByUserId(userId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()));

		return PageResponseDto.<AdminUserOrderResponseDto>builder()
				.content(orders.getContent().stream().map(this::map).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

	private AdminUserOrderResponseDto map(Order order) {

	    double subtotal = order.getItems()
	            .stream()
	            .mapToDouble(i -> i.getPrice() * i.getQuantity())
	            .sum();

	    return AdminUserOrderResponseDto.builder()
	            .orderId(order.getId())
	            .status(order.getStatus())
	            .paymentStatus(order.getPaymentStatus())
	            .paymentMethod(order.getPaymentMethod())
	            .subtotal(subtotal)
	            .taxAmount(order.getTaxAmount())
	            .shippingCharges(order.getShippingCharges())
	            .discountAmount(order.getDiscountAmount())
	            .totalAmount(order.getTotalAmount())

	            .deliveryAddress(OrderAddressDto.builder()
	                    .line1(order.getDeliveryAddressLine1())
	                    .line2(order.getDeliveryAddressLine2())
	                    .city(order.getDeliveryCity())
	                    .state(order.getDeliveryState())
	                    .postalCode(order.getDeliveryPostalCode())
	                    .country(order.getDeliveryCountry())
	                    .build())

	            .items(order.getItems().stream()
	                    .map(this::mapToAdminItem)
	                    .toList())

	            .createdAt(order.getCreatedAt())
	            .build();
	}
	private OrderItemAdminDto mapToAdminItem(OrderItem i) {
	    return OrderItemAdminDto.builder()
	            .productId(i.getProductId())
	            .variantId(i.getVariantId())
	            .productName(i.getProductName())
	            .price(i.getPrice())
	            .quantity(i.getQuantity())
	         //   .size(i.getSize())
	         //   .color(i.getColor())
	            .total(i.getPrice() * i.getQuantity())
	            .build();
	}
	
	
}
