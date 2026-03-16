package com.project.backend.service;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.WarehouseInventoryRepository;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final WarehouseInventoryRepository warehouseInventoryRepository;

	public OrderResponseDto getOrderById(Long orderId) {
		return OrderMapper.toDto(getOrder(orderId));
	}

	public PageResponseDto<OrderResponseDto> searchOrders(OrderStatus status, Long userId, Long orderId, String email,
	        String from, String to, int page, int size) {
		  try {
	    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

	    Instant fromDate = from != null ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
	    Instant toDate = to != null ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
	            : null;
	    if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email)) {
                throw new BadRequestException("Invalid email format");
            }
        }
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
	    
		  } catch (BadRequestException e) {
		        throw e;
		    } catch (Exception e) {

		        throw new RuntimeException("Failed to search orders", e);
		    }
	}
		  
		  private boolean isValidEmail(String email) {
			    if (email == null || email.trim().isEmpty()) {
			        return false;
			    }
			    String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
			    Pattern pattern = Pattern.compile(emailRegex);
			    return pattern.matcher(email).matches();
			}
		  
	@Transactional
	public OrderResponseDto updateStatus(Long orderId, OrderStatus newStatus) {
		
		try {
		  if (orderId == null) {
	            throw new BadRequestException("Order ID cannot be null");
	        }
	        if (newStatus == null) {
	            throw new BadRequestException("Order status cannot be null");
	        }

		Order order = getOrder(orderId);
		
		
		 if (order.getStatus() == newStatus) {
	            throw new BadRequestException("Order is already in " + newStatus + " status");
	        }
		validateStatusTransition(order.getStatus(), newStatus);

 if (newStatus == OrderStatus.DELIVERED && order.getStatus() != OrderStatus.DELIVERED) {
            updateInventoryOnDelivery(order);
        }

		 if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            releaseReservedStock(order);
        }


		order.setStatus(newStatus);
		return OrderMapper.toDto(orderRepository.save(order)); } catch (NotFoundException | BadRequestException e) 
		{
	        throw e;
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to update order status", e);
	    }
	}

	@Transactional
protected void releaseReservedStock(Order order) {
    
    for (OrderItem item : order.getItems()) {
        Long variantId = item.getVariantId();
        Integer quantity = item.getQuantity();
        
        List<WarehouseInventory> inventories = warehouseInventoryRepository
                .findByVariantId(variantId);
        
        int remainingToRelease = quantity;
        
        for (WarehouseInventory inventory : inventories) {
            if (remainingToRelease <= 0) break;
            
            int reserved = inventory.getReservedQuantity();
            if (reserved > 0) {
                int releaseFromThis = Math.min(reserved, remainingToRelease);
                
                inventory.setReservedQuantity(reserved - releaseFromThis);
                
                remainingToRelease -= releaseFromThis;
                
               
            }
        }
    }
    
    warehouseInventoryRepository.flush();
}

@Transactional
protected void updateInventoryOnDelivery(Order order) {
     
    for (OrderItem item : order.getItems()) {
        Long variantId = item.getVariantId();
        Integer quantity = item.getQuantity();
        
        List<WarehouseInventory> inventories = warehouseInventoryRepository
                .findByVariantId(variantId);
        
        if (inventories.isEmpty()) {
            throw new BadRequestException("Inventory not found for product: " + item.getProductName());
        }
        
        int remainingToDeduct = quantity;
        
        for (WarehouseInventory inventory : inventories) {
            if (remainingToDeduct <= 0) break;
            
            int reserved = inventory.getReservedQuantity();
            if (reserved > 0) {
                int deductFromThis = Math.min(reserved, remainingToDeduct);
                
                inventory.setAvailableQuantity(
                    inventory.getAvailableQuantity() - deductFromThis
                );
                
                inventory.setReservedQuantity(reserved - deductFromThis);
                
                remainingToDeduct -= deductFromThis;
                
			}
        }
        
        if (remainingToDeduct > 0) {
           
            throw new BadRequestException(
                "Inventory inconsistency: Not enough reserved stock for " + item.getProductName()
            );
        }
    }
    
    warehouseInventoryRepository.flush();
}


	@Transactional
	public void cancelOrder(Long orderId) {
		
	    try {
	        if (orderId == null) {
	            throw new BadRequestException("Order ID cannot be null");
	        }
		Order order = getOrder(orderId);

		 validateCancellation(order);
		

		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	    } catch (NotFoundException | BadRequestException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to cancel order", e);
	    }
	}

	
	private void validateCancellation(Order order) {
	    if (order.getStatus() == OrderStatus.CANCELLED) {
	        throw new BadRequestException("Order is already cancelled");
	    }
	    
	    if (order.getStatus() == OrderStatus.DELIVERED) {
	        throw new BadRequestException("Delivered order cannot be cancelled");
	    }
	    
	    if (order.getStatus() == OrderStatus.SHIPPED) {
	        throw new BadRequestException("Shipped order cannot be cancelled. Please request return instead.");
	    }
	    if (order.getStatus() == OrderStatus.RETURN_REQUESTED) {
	        throw new BadRequestException("Order with return request cannot be cancelled. Please process return first.");
	    }
	    
	
	}
	
	private Order getOrder(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
	}

	private void validateStatusTransition(OrderStatus current, OrderStatus next) {

	    if (current == OrderStatus.CANCELLED || current == OrderStatus.DELIVERED) {
	        throw new BadRequestException(
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
	        throw new BadRequestException(
	            "Unknown order status: " + current);
	    }
	    
	    if (!allowedNext.contains(next)) {
	        throw new BadRequestException(
	            "Invalid transition from " + current + " to " + next);
	    }
	}
	public PageResponseDto<AdminUserOrderResponseDto> getOrdersOfUser(Long userId, int page, int size) {
try {
		Page<Order> orders = orderRepository.findByUserId(userId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()));

		return PageResponseDto.<AdminUserOrderResponseDto>builder()
				.content(orders.getContent().stream().map(this::map).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
		
	   } catch (BadRequestException e) {
	        throw e;
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to fetch user orders", e);
	    }
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
