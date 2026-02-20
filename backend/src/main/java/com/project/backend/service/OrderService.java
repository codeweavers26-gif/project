package com.project.backend.service;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CheckoutResponseDto;
import com.project.backend.ResponseDto.DeliveryAddressDto;
import com.project.backend.ResponseDto.OrderItemDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Location;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;  // Already have
import com.project.backend.entity.PaymentStatus;  // Already have
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;
import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
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
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.CartItemDto;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.OrderFilter;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final CartRepository cartRepository;
	private final LocationRepository locationRepository;
	private final ProductInventoryRepository inventoryRepository;
	private final CartService cartService;
	private final UserAddressRepository userAddressRepository;
	private final UserRepository userRepository;

	@Transactional
	public CheckoutResponseDto checkout(User user, CheckoutRequestDto request) {

	    UserAddress address = userAddressRepository.findById(request.getAddressId())
	            .orElseThrow(() -> new NotFoundException("Address not found"));
	    
	    if (!address.getUser().getId().equals(user.getId())) {
	        throw new BadRequestException("Address does not belong to user");
	    }

	    Location location = locationRepository
	            .findFirstByPincodeAndIsActiveTrue(address.getPostalCode())
	            .orElseThrow(() -> new BadRequestException("Delivery not available at this postal code"));

	    // 🔴 CHANGE 1: Use payment method from request instead of hardcoding COD
	    Order order = Order.builder()
	            .user(user)
	            .location(location)
	            .deliveryAddressLine1(address.getAddressLine1())
	            .deliveryAddressLine2(address.getAddressLine2())
	            .deliveryCity(address.getCity())
	            .deliveryState(address.getState())
	            .deliveryPostalCode(address.getPostalCode())
	            .deliveryCountry(address.getCountry())
	            .paymentMethod(request.getPaymentMethod())  // ✅ From request
	            .paymentStatus(PaymentStatus.PENDING)
	            .status(OrderStatus.PENDING)
	            .totalAmount(0.0)
	            .taxAmount(0.0)
	            .shippingCharges(location.getExtraShippingCharge() != null ? location.getExtraShippingCharge() : 0.0)
	            .discountAmount(0.0)
	            .build();

	    order = orderRepository.save(order);

	    double subtotal = 0;
	    double tax = 0;

	    for (CartItemDto item : request.getItems()) {

	        Product product = productRepository.findById(item.getProductId())
	                .orElseThrow(() -> new NotFoundException("Product not found"));

	        ProductInventory inventory = inventoryRepository
	                .findByProductAndLocation(product, location)
	                .orElseThrow(() -> new BadRequestException("Product not available at your location"));

	        if (inventory.getStock() < item.getQuantity()) {
	            throw new BadRequestException(product.getName() + " out of stock");
	        }

	        // 🔴 CHANGE 2: Only deduct inventory for COD orders
	        if (request.getPaymentMethod() == PaymentMethod.COD) {
	            inventory.setStock(inventory.getStock() - item.getQuantity());
	            inventoryRepository.save(inventory);
	        }

	        OrderItem orderItem = OrderItem.builder()
	                .order(order)
	                .product(product)
	                .price(product.getPrice())
	                .quantity(item.getQuantity())
	                .build();

	        order.getItems().add(orderItem);
	        orderItemRepository.save(orderItem);

	        subtotal += product.getPrice() * item.getQuantity();
	        tax += (product.getTaxPercent() / 100) * product.getPrice() * item.getQuantity();
	    }

	    double shipping = order.getShippingCharges() != null ? order.getShippingCharges() : 0;
	    double finalAmount = subtotal + tax + shipping;

	    order.setTaxAmount(tax);
	    order.setTotalAmount(finalAmount);
	    
	    // 🔴 CHANGE 3: Different handling based on payment method
	    CheckoutResponseDto.CheckoutResponseDtoBuilder responseBuilder = CheckoutResponseDto.builder()
	            .orderId(order.getId())
	            .status(order.getStatus().name())
	            .paymentMethod(order.getPaymentMethod().name())
	            .paymentStatus(order.getPaymentStatus().name())
	            .subtotal(subtotal)
	            .taxAmount(order.getTaxAmount())
	            .shippingCharges(order.getShippingCharges())
	            .discountAmount(order.getDiscountAmount())
	            .totalAmount(order.getTotalAmount())
	            .createdAt(order.getCreatedAt())
	            .deliveryAddress(DeliveryAddressDto.builder()
	                    .addressLine1(order.getDeliveryAddressLine1())
	                    .addressLine2(order.getDeliveryAddressLine2())
	                    .city(order.getDeliveryCity())
	                    .state(order.getDeliveryState())
	                    .postalCode(order.getDeliveryPostalCode())
	                    .country(order.getDeliveryCountry())
	                    .build())
	            .items(order.getItems().stream()
	                    .map(item -> OrderItemDto.builder()
	                            .productId(item.getProduct().getId())
	                            .productName(item.getProduct().getName())
	                            .price(item.getPrice())
	                            .quantity(item.getQuantity())
	                            .total(item.getPrice() * item.getQuantity())
	                            .build())
	                    .toList());

	    if (request.getPaymentMethod() == PaymentMethod.COD) {
	        // COD: Place order immediately
	        order.setStatus(OrderStatus.PLACED);
	        orderRepository.save(order);
	        cartRepository.deleteByUser(user);
	        
	        responseBuilder
	                .requiresPayment(false)
	                .paymentMessage("Order placed successfully with Cash on Delivery");
	        
	    } else {
	        // PREPAID: Keep as PENDING, don't delete cart
	        order.setStatus(OrderStatus.PENDING);
	        orderRepository.save(order);
	        
	        responseBuilder
	                .requiresPayment(true)
	                .paymentMessage("Please complete payment to place your order");
	    }

	    return responseBuilder.build();
	}
	
	@Transactional
	public void updateOrderAfterSuccessfulPayment(Long orderId, String razorpayPaymentId) {
	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new NotFoundException("Order not found"));
	    
	    // Update order status
	    order.setPaymentStatus(PaymentStatus.SUCCESS);
	    order.setStatus(OrderStatus.PLACED);
	    orderRepository.save(order);
	    
	    // Deduct inventory (was reserved, now actually deduct)
	    for (OrderItem item : order.getItems()) {
	        ProductInventory inventory = inventoryRepository
	                .findByProductAndLocation(item.getProduct(), order.getLocation())
	                .orElseThrow(() -> new RuntimeException("Inventory not found"));
	        
	        inventory.setStock(inventory.getStock() - item.getQuantity());
	        inventoryRepository.save(inventory);
	    }
	    
	    // Clear user's cart
	    cartRepository.deleteByUser(order.getUser());
	    
	    log.info("Order {} updated after successful payment: {}", orderId, razorpayPaymentId);
	}
	
	@Transactional
	public void updateOrderAfterFailedPayment(Long orderId, String failureReason) {
	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new NotFoundException("Order not found"));
	    
	    order.setPaymentStatus(PaymentStatus.FAILED);
	    orderRepository.save(order);
	    
	    log.info("Order {} payment failed: {}", orderId, failureReason);
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

	    if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
	        throw new BadRequestException("Order cannot be cancelled now");
	    }

	    // 🔴 CHANGE: Only restore inventory if it was deducted
	    if (order.getPaymentMethod() == PaymentMethod.COD || 
	        order.getPaymentStatus() == PaymentStatus.SUCCESS) {
	        // Restore inventory
	        for (OrderItem item : order.getItems()) {
	            ProductInventory inventory = inventoryRepository
	                    .findByProductAndLocation(item.getProduct(), order.getLocation())
	                    .orElseThrow(() -> new RuntimeException("Inventory not found"));

	            inventory.setStock(inventory.getStock() + item.getQuantity());
	            inventoryRepository.save(inventory);
	        }
	    }

	    order.setStatus(OrderStatus.CANCELLED);
	    order.setPaymentStatus(order.getPaymentMethod() == PaymentMethod.PREPAID && 
	                          order.getPaymentStatus() == PaymentStatus.SUCCESS ? 
	                          PaymentStatus.REFUND_PENDING : PaymentStatus.CANCELLED);

	    orderRepository.save(order);
	}
	private CheckoutResponseDto mapToCheckoutResponse(Order order, double subtotal) {

		return CheckoutResponseDto.builder().orderId(order.getId()).status(order.getStatus().name())
				.paymentMethod(order.getPaymentMethod().name()).paymentStatus(order.getPaymentStatus().name())
				.subtotal(subtotal).taxAmount(order.getTaxAmount()).shippingCharges(order.getShippingCharges())
				.discountAmount(order.getDiscountAmount()).totalAmount(order.getTotalAmount())
				.createdAt(order.getCreatedAt())

				.deliveryAddress(DeliveryAddressDto.builder().addressLine1(order.getDeliveryAddressLine1())
						.addressLine2(order.getDeliveryAddressLine2()).city(order.getDeliveryCity())
						.state(order.getDeliveryState()).postalCode(order.getDeliveryPostalCode())
						.country(order.getDeliveryCountry()).build())

				.items(order.getItems().stream()
						.map(item -> OrderItemDto.builder().productId(item.getProduct().getId())
								.productName(item.getProduct().getName()).price(item.getPrice())
								.quantity(item.getQuantity()).total(item.getPrice() * item.getQuantity()).build())
						.toList())
				.build();
	}
	
	public OrderResponseDto getOrderById(Long orderId, User user) {

	    Order order = orderRepository.findById(orderId)
	        .orElseThrow(() -> new NotFoundException("Order not found"));

	    if (!order.getUser().getId().equals(user.getId())) {
	        throw new UnauthorizedException("Not your order");
	    }

	    return OrderMapper.toDto(order);
	}
	@Transactional
	public void reorder(User user, Long orderId) {

	    Order order = orderRepository.findById(orderId)
	        .orElseThrow(() -> new NotFoundException("Order not found"));

	    if (!order.getUser().getId().equals(user.getId())) {
	        throw new UnauthorizedException("Not your order");
	    }

	    for (OrderItem item : order.getItems()) {
	    	cartService.addOrUpdate(user, item.getProduct(), item.getQuantity());

	    }
	}
	  public PageResponseDto<OrderResponseDto> getUserOrdersWithFilters(
	            OrderFilter filter, int page, int size) {
	        
	        // Validate user exists
	        if (filter.getUserId() != null) {
	            userRepository.findById(filter.getUserId())
	                .orElseThrow(() -> new NotFoundException("User not found"));
	        }
	        
	        // Parse dates
	        Instant from = parseDate(filter.getFromDate(), false);
	        Instant to = parseDate(filter.getToDate(), true);
	        
	        // Parse enums
	        OrderStatus orderStatus = filter.getStatus() != null ? 
	            OrderStatus.valueOf(filter.getStatus().toUpperCase()) : null;
	        
	        PaymentStatus paymentStatus = filter.getPaymentStatus() != null ? 
	            PaymentStatus.valueOf(filter.getPaymentStatus().toUpperCase()) : null;
	        
	        PaymentMethod paymentMethod = filter.getPaymentMethod() != null ? 
	            PaymentMethod.valueOf(filter.getPaymentMethod().toUpperCase()) : null;
	        
	        // Create sort
	        Sort sort = Sort.by(
	            Sort.Direction.fromString(filter.getSortDirection()), 
	            filter.getSortBy()
	        );
	        PageRequest pageable = PageRequest.of(page, size, sort);
	        
	        // Get filtered orders
	        Page<Order> orders = orderRepository.findOrdersByFilters(
	            filter.getUserId(),
	            orderStatus,
	            paymentStatus,
	            paymentMethod,
	            filter.getMinAmount(),
	            filter.getMaxAmount(),
	            from,
	            to,
	            filter.getSearch(),
	            pageable
	        );
	        
	        return PageResponseDto.<OrderResponseDto>builder()
	                .content(orders.getContent().stream()
	                        .map(OrderMapper::toDto)
	                        .toList())
	                .page(orders.getNumber())
	                .size(orders.getSize())
	                .totalElements(orders.getTotalElements())
	                .totalPages(orders.getTotalPages())
	                .last(orders.isLast())
	                .build();
	    }
	    
	    private Instant parseDate(String dateStr, boolean endOfDay) {
	        if (dateStr == null) return null;
	        try {
	            LocalDate date = LocalDate.parse(dateStr);
	            if (endOfDay) {
	                return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
	            }
	            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
	        } catch (Exception e) {
	            throw new BadRequestException("Invalid date format. Use yyyy-MM-dd");
	        }
	    }


}
