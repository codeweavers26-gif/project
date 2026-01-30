package com.project.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Location;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.ProductInventoryRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.CartItemDto;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final CartRepository cartRepository;
	private final LocationRepository locationRepository;
	private final ProductInventoryRepository inventoryRepository;

	@Transactional
	public Order checkout(User user, CheckoutRequestDto request) {

		Location location = locationRepository.findById(request.getLocationId())
				.orElseThrow(() -> new NotFoundException("Location not found"));

		Order order = orderRepository
				.save(Order.builder().user(user).status(OrderStatus.PENDING).totalAmount(0.0).build());

		double total = 0;

		for (CartItemDto item : request.getItems()) {

			Product product = productRepository.findById(item.getProductId())
					.orElseThrow(() -> new NotFoundException("Product not found"));

			ProductInventory inventory = inventoryRepository.findByProductAndLocation(product, location)
					.orElseThrow(() -> new BadRequestException("Product not available at this location"));

			if (inventory.getStock() < item.getQuantity()) {
				throw new com.project.backend.exception.BadRequestException(product.getName() + " out of stock at this location");
			}

			inventory.setStock(inventory.getStock() - item.getQuantity());
			inventoryRepository.save(inventory);

//			orderItemRepository.save
//			(OrderItem.builder().order(order).product(product) .location(location) .price(product.getPrice())
//					.quantity(item.getQuantity()).build());

			total += product.getPrice() * item.getQuantity();
		}

		order.setTotalAmount(total);
		order.setStatus(OrderStatus.PAID);
		return orderRepository.save(order);
	}

	public PageResponseDto<OrderResponseDto> loggedUserLogin(User user, int page, int size) {

		PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<Order> orders = orderRepository.findByUser(user, pageable);

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();

	}
	
	@Transactional
	public void cancelOrder(Long orderId, User user) {

	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new NotFoundException("Order not found"));

	    if (!order.getUser().getId().equals(user.getId())) {
	        throw new UnauthorizedException("Not your order");
	    }

	    if (order.getStatus() == OrderStatus.SHIPPED ||
	        order.getStatus() == OrderStatus.DELIVERED) {
	        throw new BadRequestException("Order cannot be cancelled now");
	    }

	    // Restore inventory
	    for (OrderItem item : order.getItems()) {

	        ProductInventory inventory =
	                inventoryRepository.findByProductAndLocation(
	                        item.getProduct(),
	                        order.getLocation()
	                ).orElseThrow(() -> new RuntimeException("Inventory not found"));

	        inventory.setStock(
	                inventory.getStock() + item.getQuantity()
	        );

	        inventoryRepository.save(inventory);
	    }

	    order.setStatus(OrderStatus.CANCELLED);
	    orderRepository.save(order);
	}


}
