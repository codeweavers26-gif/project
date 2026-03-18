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

		Double extraShippingCharge = null;
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
		int maxDeliveryDays = 0;
		  boolean isCodAvailable = true; 
		for (CartItem item : cart.getItems()) {

			ProductVariant variant = item.getVariant();
			Product product = item.getProduct();
			Integer qty = item.getQuantity();
			Integer productDeliveryDays = product.getDeliveryDays();

			if (productDeliveryDays == null) {
				log.warn("Product {} has no delivery days set", product.getId());
				productDeliveryDays = 10;

			}
			if (productDeliveryDays > maxDeliveryDays) {
				maxDeliveryDays = productDeliveryDays;
				log.debug("New max delivery days: {} from product: {}", maxDeliveryDays, product.getName());
			}
			
			  Boolean productCodAvailable = product.getCodAvailable();
		        if (productCodAvailable == null) {
		            productCodAvailable = true; 
		        }
		        

			List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

			int totalAvailable = inventories.stream().mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
					.sum();

			boolean inStock = totalAvailable >= qty;
			if (!inStock) {
				isValidForCheckout = false;
				validationErrors.add(String.format("%s is out of stock. Only %d available, you have %d in cart",
						variant.getSku(), totalAvailable, qty));
			}

			BigDecimal price = variant.getSellingPrice();
			BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
			BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));

			BigDecimal gstPercent = BigDecimal.valueOf(5);
			BigDecimal itemTax = itemSubtotal.multiply(gstPercent).divide(BigDecimal.valueOf(100));

			subtotal = subtotal.add(itemSubtotal);
			taxTotal = taxTotal.add(itemTax);

			int discountPercentage = 0;
			if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
				discountPercentage = mrp.subtract(price).multiply(BigDecimal.valueOf(100))
						.divide(mrp, 0, RoundingMode.HALF_UP).intValue();
			}

			itemDtos.add(CheckoutResponseDto.CheckoutItemDto.builder().productId(product.getId())
					.productName(product.getName()).productImage(getProductImage(product)).variantId(variant.getId())
					.size(variant.getSize()).color(variant.getColor()).sku(variant.getSku()).quantity(qty).price(price)
					.mrp(mrp).discountPercentage(discountPercentage).subtotal(itemSubtotal).inStock(inStock)
					.availableStock(totalAvailable).build());
		}

		BigDecimal shipping;
		if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
			shipping = BigDecimal.ZERO;
		} else {
			shipping = BigDecimal.valueOf(extraShippingCharge);
		}

		BigDecimal grandTotal = subtotal.add(taxTotal).add(shipping);
		LocalDate expectedDelivery = LocalDate.now().plusDays(maxDeliveryDays);

		CheckoutResponseDto.AddressDto addressDto = CheckoutResponseDto.AddressDto.builder()

				.addressLine1(address.getAddressLine1()).addressLine2(address.getAddressLine2()).city(address.getCity())
				.state(address.getState()).postalCode(address.getPostalCode()).country(address.getCountry()).build();

		return CheckoutResponseDto.builder().subtotal(subtotal).taxAmount(taxTotal).shippingCharges(shipping)
				.discountAmount(BigDecimal.ZERO).totalAmount(grandTotal).items(itemDtos)
				.totalItems(cart.getTotalQuantity()).deliveryAddress(addressDto).deliveryDays(maxDeliveryDays)
				.expectedDelivery(expectedDelivery).isDeliveryAvailable(true).paymentMethod(request.getPaymentMethod())
				.requiresPayment(request.getPaymentMethod() != PaymentMethod.COD).paymentMessage(null)
				.isCodAvailable(isCodAvailable)
				.cartId(cart.getId()).isValidForCheckout(isValidForCheckout).validationErrors(validationErrors).build();
	}

	private BigDecimal calculateGST(BigDecimal amount, ProductVariant variant) {
	    BigDecimal gstPercent = getGSTPercentage(variant);
	    return amount.multiply(gstPercent).divide(BigDecimal.valueOf(100));
	}

	private BigDecimal getGSTPercentage(ProductVariant variant) {
	    return BigDecimal.valueOf(5); 
	}
	
	private BigDecimal calculateShipping(BigDecimal subtotal, Location location) {
	    BigDecimal shipping = BigDecimal.valueOf(
	        location.getExtraShippingCharge() != null ? location.getExtraShippingCharge() : 0.0
	    );
	    
	    BigDecimal freeShippingThreshold = BigDecimal.valueOf(999);
	    
	    if (subtotal.compareTo(freeShippingThreshold) > 0) {
	        return BigDecimal.ZERO;
	    }
	    return shipping;
	}
	
	private String getProductImage(Product product) {
		if (product.getImages() != null && !product.getImages().isEmpty()) {
			return product.getImages().get(0).getImageUrl();
		}
		return null;
	}

	@Transactional
	public PlaceOrderResponseDto placeOrder(User user, Long addressId, PaymentMethod paymentMethod) {
	    try {
	        if (user == null) {
	            throw new BadRequestException("User not authenticated");
	        }
	        
	        if (addressId == null) {
	            throw new BadRequestException("Address ID is required");
	        }
	        
	        if (paymentMethod == null) {
	            throw new BadRequestException("Payment method is required");
	        }

	        UserAddress address = userAddressRepository.findById(addressId)
	                .orElseThrow(() -> new NotFoundException("Address not found with ID: " + addressId));

	        if (!address.getUser().getId().equals(user.getId())) {
	            throw new BadRequestException("Address does not belong to the current user");
	        }

	        Cart cart = cartRepository.findByUserId(user.getId())
	                .orElseThrow(() -> new BadRequestException("Cart is empty"));

	        if (cart.getItems() == null || cart.getItems().isEmpty()) {
	            throw new BadRequestException("Cart has no items");
	        }

	        Warehouse defaultWarehouse = warehouseRepository.findById(1L)
	                .orElseThrow(() -> new NotFoundException("Default warehouse not configured. Please contact support."));

	        BigDecimal subtotal = BigDecimal.ZERO;
	        BigDecimal taxTotal = BigDecimal.ZERO;
	        BigDecimal shipping = BigDecimal.valueOf(12);
	        int maxDeliveryDays = 0;

	        Order order = Order.builder()
	                .user(user)
	                .shippingAddressId(addressId)
	                .deliveryAddressLine1(address.getAddressLine1())
	                .deliveryAddressLine2(address.getAddressLine2())
	                .deliveryCity(address.getCity())
	                .deliveryState(address.getState())
	                .deliveryPostalCode(address.getPostalCode())
	                .deliveryCountry(address.getCountry())
	                .paymentMethod(paymentMethod)
	                .status(paymentMethod == PaymentMethod.COD ? 
	                        OrderStatus.PENDING : OrderStatus.PENDING_PAYMENT)
	                .paymentStatus(PaymentStatus.PENDING)
	                .paymentExpiry(paymentMethod == PaymentMethod.PREPAID ? 
	                               LocalDateTime.now().plusMinutes(15) : null)
	                .paymentExpiry(LocalDateTime.now().plusMinutes(15))
	                .taxAmount(0.0)
	                .warehouse(defaultWarehouse)
	                .shippingCharges(shipping.doubleValue())
	                .totalAmount(0.0)
	                .build();

	        order = orderRepository.save(order);

	        for (CartItem item : cart.getItems()) {
	            try {
	                if (item.getVariant() == null) {
	                    throw new BadRequestException("Invalid cart item: missing variant information");
	                }
	                
	                if (item.getProduct() == null) {
	                    throw new BadRequestException("Invalid cart item: missing product information");
	                }

	                ProductVariant variant = item.getVariant();
	                int quantity = item.getQuantity();
	                Product product = item.getProduct();

	                Integer deliveryDays = product.getDeliveryDays();
	                if (deliveryDays == null) {
	                    log.warn("Product ID: {} has null delivery days. Using default value of 5 days", product.getId());
	                    deliveryDays = 5; 
	                }
	                maxDeliveryDays = Math.max(maxDeliveryDays, deliveryDays);

	                List<WarehouseInventory> inventories;
	                try {
	                    inventories = inventoryRepository.findByVariantIdForUpdate(variant.getId());
	                } catch (Exception e) {
	                    log.error("Error fetching inventory for variant ID: {}", variant.getId(), e);
	                    throw new BadRequestException("Unable to check inventory. Please try again.");
	                }

	                if (inventories == null || inventories.isEmpty()) {
	                    throw new BadRequestException("Product variant " + variant.getSku() + " is out of stock");
	                }

	                int available = inventories.stream()
	                        .mapToInt(i -> {
	                            int availableQty = i.getAvailableQuantity() - i.getReservedQuantity();
	                            return Math.max(availableQty, 0); 
	                        })
	                        .sum();

	                if (available < quantity) {
	                    throw new BadRequestException("Insufficient stock for " + variant.getSku() + 
	                            ". Available: " + available + ", Requested: " + quantity);
	                }

	                BigDecimal price = variant.getSellingPrice();
	                if (price == null) {
	                    log.error("Variant ID: {} has null selling price", variant.getId());
	                    throw new BadRequestException("Product price not configured");
	                }

	                BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity));
	                
	                BigDecimal gstPercent;
	                try {
	                    gstPercent = BigDecimal.valueOf(product.getTaxPercent() != null ? product.getTaxPercent() : 5);
	                } catch (Exception e) {
	                    log.warn("Error getting tax percent for product ID: {}, using default 5%", product.getId());
	                    gstPercent = BigDecimal.valueOf(5);
	                }
	                
	                BigDecimal itemTax = itemSubtotal.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

	                subtotal = subtotal.add(itemSubtotal);
	                taxTotal = taxTotal.add(itemTax);
	                OrderItem orderItem = OrderItem.builder()
	                        .order(order)
	                        .productId(variant.getProduct().getId())
	                        .variantId(variant.getId())
	                        .productName(variant.getProduct().getName())
	                        .price(variant.getSellingPrice().doubleValue())
	                        .quantity(quantity)
	                        .size(variant.getSize())
	                        .color(variant.getColor())
	                        .build();

	                try {
	                    orderItemRepository.save(orderItem);
	                } catch (Exception e) {
	                    log.error("Failed to save order item for variant ID: {}", variant.getId(), e);
	                    throw new BadRequestException("Failed to process order item. Please try again.");
	                }

	                try {
	                    reserveStock(inventories, quantity);
	                } catch (Exception e) {
	                    log.error("Failed to reserve stock for variant ID: {}", variant.getId(), e);
	                    throw new BadRequestException("Failed to reserve stock. Please try again.");
	                }

	            } catch (BadRequestException e) {
	                throw e;
	            } catch (Exception e) {
	                log.error("Unexpected error processing cart item: {}", item.getId(), e);
	                throw new BadRequestException("Error processing cart item: " + e.getMessage());
	            }
	        }

	        if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
	            shipping = BigDecimal.ZERO;
	        }

	        BigDecimal grandTotal = subtotal.add(taxTotal).add(shipping);

	        order.setTaxAmount(taxTotal.doubleValue());
	        order.setShippingCharges(shipping.doubleValue());
	        order.setTotalAmount(grandTotal.doubleValue());

	        try {
	            orderRepository.save(order);
	        } catch (Exception e) {
	            log.error("Failed to update order with ID: {}", order.getId(), e);
	            throw new BadRequestException("Failed to finalize order. Please try again.");
	        }

	        try {
	            cart.getItems().clear();
	            cartRepository.save(cart);
	        } catch (Exception e) {
	            log.warn("Failed to clear cart after order placement. Order ID: {}", order.getId());

	        }

	        return PlaceOrderResponseDto.builder()
	                .orderId(order.getId())
	                .subtotal(subtotal)
	                .taxAmount(taxTotal)
	                .shippingCharges(shipping)
	                .totalAmount(grandTotal)
	                .orderStatus(order.getStatus() != null ? order.getStatus().name() : "PENDING")
	                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING")
	                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
	                .expectedDelivery(LocalDate.now().plusDays(maxDeliveryDays))
	                .deliveryDays(maxDeliveryDays)
	                .paymentExpiry(order.getPaymentExpiry())
	                .requiresPayment(paymentMethod != PaymentMethod.COD)
	                .deliveryAddress(PlaceOrderResponseDto.DeliveryAddressDto.builder()
	                        .addressLine1(address.getAddressLine1())
	                        .addressLine2(address.getAddressLine2())
	                        .city(address.getCity())
	                        .state(address.getState())
	                        .postalCode(address.getPostalCode())
	                        .country(address.getCountry())
	                        .build())
	                .message("Order placed successfully")
	                .build();

	    } catch (BadRequestException | NotFoundException e) {
	        throw e;
	    } catch (Exception e) {
	        log.error("Unexpected error in placeOrder for user ID: {}", user != null ? user.getId() : "unknown", e);
	        throw new BadRequestException("Failed to place order: " + e.getMessage());
	    }
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

	}

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

		if (filter.getUserId() != null) {
			userRepository.findById(filter.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));
		}

		Instant from = parseDate(filter.getFromDate(), false);
		Instant to = parseDate(filter.getToDate(), true);

		OrderStatus orderStatus = filter.getStatus() != null ? OrderStatus.valueOf(filter.getStatus().toUpperCase())
				: null;

		PaymentStatus paymentStatus = filter.getPaymentStatus() != null
				? PaymentStatus.valueOf(filter.getPaymentStatus().toUpperCase())
				: null;

		PaymentMethod paymentMethod = filter.getPaymentMethod() != null
				? PaymentMethod.valueOf(filter.getPaymentMethod().toUpperCase())
				: null;

		Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
		PageRequest pageable = PageRequest.of(page, size, sort);
		String paymentMethodString = paymentMethod != null ? paymentMethod.name() : null;

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
