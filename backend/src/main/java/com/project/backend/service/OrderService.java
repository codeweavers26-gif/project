package com.project.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CheckoutResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.PlaceOrderResponseDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.CartItem;
import com.project.backend.entity.Location;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import com.project.backend.entity.Warehouse;
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.WarehouseInventoryRepository;
import com.project.backend.repository.WarehouseRepository;
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
	private final WarehouseInventoryRepository inventoryRepository;

	private final WarehouseRepository warehouseRepository;
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

		Location location = locationRepository.findFirstByPincodeAndIsActiveTrue(address.getPostalCode())
				.orElseThrow(() -> new BadRequestException("Delivery not available at this postal code"));

		Integer deliveryDays = location.getDeliveryDays();
		Double extraShippingCharge = location.getExtraShippingCharge();
		if (extraShippingCharge == null) {
			extraShippingCharge = 0.0;
		}

		Cart cart = cartRepository.findByUserId(user.getId())
				.orElseThrow(() -> new BadRequestException("Cart is empty"));

		if (cart.getItems().isEmpty()) {
			throw new BadRequestException("Cart is empty");
		}

		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal taxTotal = BigDecimal.ZERO;
		List<CheckoutResponseDto.CheckoutItemDto> itemDtos = new ArrayList<>();
		List<String> validationErrors = new ArrayList<>();
		boolean isValidForCheckout = true;

		for (CartItem item : cart.getItems()) {

			ProductVariant variant = item.getVariant();
			Product product = item.getProduct();
			Integer qty = item.getQuantity();
			List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

			int totalAvailable = inventories.stream().mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
					.sum();

			// Stock validation
			boolean inStock = totalAvailable >= qty;
			if (!inStock) {
				isValidForCheckout = false;
				validationErrors.add(String.format("%s is out of stock. Only %d available, you have %d in cart",
						variant.getSku(), totalAvailable, qty));
			}

			BigDecimal price = variant.getSellingPrice();
			BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
			BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));

			// Tax calculation
			BigDecimal gstPercent = BigDecimal.valueOf(5);
			BigDecimal itemTax = itemSubtotal.multiply(gstPercent).divide(BigDecimal.valueOf(100));

			subtotal = subtotal.add(itemSubtotal);
			taxTotal = taxTotal.add(itemTax);

			// Calculate discount percentage
			int discountPercentage = 0;
			if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
				discountPercentage = mrp.subtract(price).multiply(BigDecimal.valueOf(100))
						.divide(mrp, 0, RoundingMode.HALF_UP).intValue();
			}

			// Build item DTO
			itemDtos.add(CheckoutResponseDto.CheckoutItemDto.builder().productId(product.getId())
					.productName(product.getName()).productImage(getProductImage(product)).variantId(variant.getId())
					.size(variant.getSize()).color(variant.getColor()).sku(variant.getSku()).quantity(qty).price(price)
					.mrp(mrp).discountPercentage(discountPercentage).subtotal(itemSubtotal).inStock(inStock)
					.availableStock(totalAvailable).build());
		}

		// Calculate shipping
		BigDecimal shipping;
		if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
			shipping = BigDecimal.ZERO;
		} else {
			shipping = BigDecimal.valueOf(extraShippingCharge);
		}

		BigDecimal grandTotal = subtotal.add(taxTotal).add(shipping);
		LocalDate expectedDelivery = LocalDate.now().plusDays(deliveryDays);

		// Build address DTO
		CheckoutResponseDto.AddressDto addressDto = CheckoutResponseDto.AddressDto.builder()

				.addressLine1(address.getAddressLine1()).addressLine2(address.getAddressLine2()).city(address.getCity())
				.state(address.getState()).postalCode(address.getPostalCode()).country(address.getCountry()).build();

		return CheckoutResponseDto.builder().subtotal(subtotal).taxAmount(taxTotal).shippingCharges(shipping)
				.discountAmount(BigDecimal.ZERO).totalAmount(grandTotal).items(itemDtos)
				.totalItems(cart.getTotalQuantity()).deliveryAddress(addressDto).deliveryDays(deliveryDays)
				.expectedDelivery(expectedDelivery).isDeliveryAvailable(true).paymentMethod(request.getPaymentMethod())
				.requiresPayment(request.getPaymentMethod() != PaymentMethod.COD).paymentMessage(null)
				.isCodAvailable(location.getCodAvailable() != null ? location.getCodAvailable() : false)
				.cartId(cart.getId()).isValidForCheckout(isValidForCheckout).validationErrors(validationErrors).build();
	}

	// Helper method for product image
	private String getProductImage(Product product) {
		if (product.getImages() != null && !product.getImages().isEmpty()) {
			return product.getImages().get(0).getImageUrl();
		}
		return null;
	}

	@Transactional
	public PlaceOrderResponseDto placeOrder(User user, Long addressId, PaymentMethod paymentMethod) {

		UserAddress address = userAddressRepository.findById(addressId)
				.orElseThrow(() -> new NotFoundException("Address not found"));

		if (!address.getUser().getId().equals(user.getId())) {
			throw new BadRequestException("Address does not belong to user");
		}

		Location location = locationRepository.findFirstByPincodeAndIsActiveTrue(address.getPostalCode())
				.orElseThrow(() -> new BadRequestException("Delivery not available"));

		Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow(() -> new BadRequestException("Cart empty"));

		if (cart.getItems().isEmpty()) {
			throw new BadRequestException("Cart empty");
		}

		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal taxTotal = BigDecimal.ZERO;
		BigDecimal shipping = BigDecimal
				.valueOf(location.getExtraShippingCharge() != null ? location.getExtraShippingCharge() : 0.0);

		Warehouse defaultWarehouse = warehouseRepository.findById(1L)
				.orElseThrow(() -> new NotFoundException("Default warehouse not found"));

		Order order = Order.builder().user(user).location(location).shippingAddressId(addressId)
				.deliveryAddressLine1(address.getAddressLine1()).deliveryAddressLine2(address.getAddressLine2())
				.deliveryCity(address.getCity()).deliveryState(address.getState())
				.deliveryPostalCode(address.getPostalCode()).deliveryCountry(address.getCountry())
				.paymentMethod(paymentMethod).status(OrderStatus.PENDING_PAYMENT).paymentStatus(PaymentStatus.PENDING)
				.paymentExpiry(LocalDateTime.now().plusMinutes(15)).taxAmount(0.0).warehouse(defaultWarehouse)
//	            .cgstAmount(0.0)  
//	            .igstAmount(0.0) 
//	            .sgstAmount(0.0) 
				.shippingCharges(shipping.doubleValue()).totalAmount(0.0).build();

		order = orderRepository.save(order);

		for (CartItem item : cart.getItems()) {

			ProductVariant variant = item.getVariant();
			int qty = item.getQuantity();

			List<WarehouseInventory> inventories = inventoryRepository.findByVariantIdForUpdate(variant.getId());

			int available = inventories.stream().mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
					.sum();

			if (available < qty) {
				throw new BadRequestException(variant.getSku() + " out of stock");
			}

			BigDecimal price = variant.getSellingPrice();

			BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));
			BigDecimal gstPercent = BigDecimal.valueOf(5);
			BigDecimal itemTax = itemSubtotal.multiply(gstPercent).divide(BigDecimal.valueOf(100));

			subtotal = subtotal.add(itemSubtotal);
			taxTotal = taxTotal.add(itemTax);

			OrderItem orderItem = OrderItem.builder().order(order).productId(variant.getProduct().getId())
					.variantId(variant.getId()).productName(variant.getProduct().getName())
					.price(variant.getSellingPrice().doubleValue()).quantity(qty).size(variant.getSize())
					.color(variant.getColor()).build();

			orderItemRepository.save(orderItem);

			reserveStock(inventories, qty);
		}

		if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
			shipping = BigDecimal.ZERO;
		}

		BigDecimal grandTotal = subtotal.add(taxTotal).add(shipping);

		order.setTaxAmount(taxTotal.doubleValue());
		order.setShippingCharges(shipping.doubleValue());
		order.setTotalAmount(grandTotal.doubleValue());

		orderRepository.save(order);

		return PlaceOrderResponseDto.builder().orderId(order.getId())
				.subtotal(subtotal).taxAmount(taxTotal).shippingCharges(shipping)
			//	.discountAmount(BigDecimal.valueOf(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0))
				.totalAmount(grandTotal).orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
				.paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
				.paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
				.expectedDelivery(LocalDate.now().plusDays(location.getDeliveryDays()))
				.deliveryDays(location.getDeliveryDays()).paymentExpiry(order.getPaymentExpiry())
				.requiresPayment(paymentMethod != PaymentMethod.COD)
				.deliveryAddress(PlaceOrderResponseDto.DeliveryAddressDto.builder()
						.addressLine1(address.getAddressLine1()).addressLine2(address.getAddressLine2())
						.city(address.getCity()).state(address.getState()).postalCode(address.getPostalCode())
						.country(address.getCountry()).build())
				.message("Order placed successfully").build();
	}

	private void reserveStock(List<WarehouseInventory> inventories, int qty) {

		for (WarehouseInventory inv : inventories) {

			int available = inv.getAvailableQuantity() - inv.getReservedQuantity();

			if (available <= 0)
				continue;

			int reserve = Math.min(available, qty);

			inv.setReservedQuantity(inv.getReservedQuantity() + reserve);
			inventoryRepository.save(inv);

			qty -= reserve;

			if (qty == 0)
				break;
		}

		if (qty > 0) {
			throw new BadRequestException("Unable to reserve full stock");
		}
	}

	@Transactional
	public void updateOrderAfterSuccessfulPayment(Long orderId, String razorpayPaymentId) {
//		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
//
//		// Update order status
//		order.setPaymentStatus(PaymentStatus.SUCCESS);
//		order.setStatus(OrderStatus.PLACED);
//		orderRepository.save(order);
//
//		// Deduct inventory (was reserved, now actually deduct)
//		for (OrderItem item : order.getItems()) {
//			ProductInventory inventory = inventoryRepository
//					.findByProductAndLocation(item.getProduct(), order.getLocation())
//					.orElseThrow(() -> new RuntimeException("Inventory not found"));
//
//			inventory.setStock(inventory.getStock() - item.getQuantity());
//			inventoryRepository.save(inventory);
//		}
//
//		// Clear user's cart
//		cartRepository.deleteByUser(order.getUser());
//
//		log.info("Order {} updated after successful payment: {}", orderId, razorpayPaymentId);
	}

	@Transactional
	public void updateOrderAfterFailedPayment(Long orderId, String failureReason) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

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
//
//		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
//
//		if (!order.getUser().getId().equals(user.getId())) {
//			throw new UnauthorizedException("Not your order");
//		}
//
//		if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
//			throw new BadRequestException("Order cannot be cancelled now");
//		}
//
//		// 🔴 CHANGE: Only restore inventory if it was deducted
//		if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.SUCCESS) {
//			// Restore inventory
//			for (OrderItem item : order.getItems()) {
//				ProductInventory inventory = inventoryRepository
//						.findByProductAndLocation(item.getProduct(), order.getLocation())
//						.orElseThrow(() -> new RuntimeException("Inventory not found"));
//
//				inventory.setStock(inventory.getStock() + item.getQuantity());
//				inventoryRepository.save(inventory);
//			}
//		}
//
//		order.setStatus(OrderStatus.CANCELLED);
//		order.setPaymentStatus(
//				order.getPaymentMethod() == PaymentMethod.PREPAID && order.getPaymentStatus() == PaymentStatus.SUCCESS
//						? PaymentStatus.REFUND_PENDING
//						: PaymentStatus.CANCELLED);
//
//		orderRepository.save(order);
	}

//	private CheckoutResponseDto mapToCheckoutResponse(Order order, double subtotal) {
//
//	    return CheckoutResponseDto.builder()
//	            .orderId(order.getId())
//	            .status(order.getStatus().name())
//	            .paymentMethod(order.getPaymentMethod().name())
//	            .paymentStatus(order.getPaymentStatus().name())
//	            .subtotal(subtotal)
//	            .taxAmount(order.getTaxAmount())
//	            .shippingCharges(order.getShippingCharges())
//	            .discountAmount(order.getDiscountAmount())
//	            .totalAmount(order.getTotalAmount())
//	            .createdAt(order.getCreatedAt())
//
//	            .deliveryAddress(
//	                    DeliveryAddressDto.builder()
//	                            .addressLine1(order.getDeliveryAddressLine1())
//	                            .addressLine2(order.getDeliveryAddressLine2())
//	                            .city(order.getDeliveryCity())
//	                            .state(order.getDeliveryState())
//	                            .postalCode(order.getDeliveryPostalCode())
//	                            .country(order.getDeliveryCountry())
//	                            .build()
//	            )
//
//	            .items(order.getItems().stream()
//	                    .map(item -> OrderItemDto.builder()
//	                            .productId(item.getProductId())
//	                            .variantId(item.getVariantId())
//	                            .productName(item.getProductName())
//	                            .price(item.getPrice())
//	                            .quantity(item.getQuantity())
//	                            .size(item.getSize())
//	                            .color(item.getColor())
//	                            .total(item.getPrice() * item.getQuantity())
//	                            .build())
//	                    .toList()
//	            )
//	            .build();
//	}
	public OrderResponseDto getOrderById(Long orderId, User user) {

		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

		if (!order.getUser().getId().equals(user.getId())) {
			throw new UnauthorizedException("Not your order");
		}

		return OrderMapper.toDto(order);
	}

	@Transactional
	public void reorder(User user, Long orderId) {

		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

		if (!order.getUser().getId().equals(user.getId())) {
			throw new UnauthorizedException("Not your order");
		}

		for (OrderItem item : order.getItems()) {
//	    	cartService.addOrUpdate(user, item.getProduct(), item.getQuantity());

		}
	}

	public PageResponseDto<OrderResponseDto> getUserOrdersWithFilters(OrderFilter filter, int page, int size) {

		// Validate user exists
		if (filter.getUserId() != null) {
			userRepository.findById(filter.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));
		}

		// Parse dates
		Instant from = parseDate(filter.getFromDate(), false);
		Instant to = parseDate(filter.getToDate(), true);

		// Parse enums
		OrderStatus orderStatus = filter.getStatus() != null ? OrderStatus.valueOf(filter.getStatus().toUpperCase())
				: null;

		PaymentStatus paymentStatus = filter.getPaymentStatus() != null
				? PaymentStatus.valueOf(filter.getPaymentStatus().toUpperCase())
				: null;

		PaymentMethod paymentMethod = filter.getPaymentMethod() != null
				? PaymentMethod.valueOf(filter.getPaymentMethod().toUpperCase())
				: null;

		// Create sort
		Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
		PageRequest pageable = PageRequest.of(page, size, sort);
		String paymentMethodString = paymentMethod != null ? paymentMethod.name() : null;

		// Get filtered orders
		Page<Order> orders = orderRepository.findOrdersByFilters(filter.getUserId(), orderStatus, filter.getMinAmount(),
				filter.getMaxAmount(), from, to, filter.getSearch(), pageable);

		return PageResponseDto.<OrderResponseDto>builder()
				.content(orders.getContent().stream().map(OrderMapper::toDto).toList()).page(orders.getNumber())
				.size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
				.last(orders.isLast()).build();
	}

	private Instant parseDate(String dateStr, boolean endOfDay) {
		if (dateStr == null)
			return null;
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
