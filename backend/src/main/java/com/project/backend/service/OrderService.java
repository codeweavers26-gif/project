package com.project.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.project.backend.ResponseDto.CheckoutResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.PlaceOrderResponseDto;
import com.project.backend.ResponseDto.ServiceabilityResponse;
import com.project.backend.ResponseDto.ShipmentResponse;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.ResponseDto.TrackingResponseDto;
import com.project.backend.config.RetryUtil;
import com.project.backend.config.ShippingFactory;
import com.project.backend.config.ShippingProvider;
import com.project.backend.entity.Cart;
import com.project.backend.entity.CartItem;
import com.project.backend.entity.IdempotencyKey;
import com.project.backend.entity.Location;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.Shipment;
import com.project.backend.entity.ShippingProviderType;
import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import com.project.backend.entity.Warehouse;
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.mapper.OrderMapper;
import com.project.backend.repository.CartItemRepository;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.IdempotencyKeyRepository;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.ProductVariantRepository;
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.repository.WarehouseInventoryRepository;
import com.project.backend.repository.WarehouseRepository;
import com.project.backend.requestDto.BuyNowCheckoutResponseDto;
import com.project.backend.requestDto.BuyNowRequestDto;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.OrderFilter;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.repository.OrderStatusHistoryRepository;
import com.project.backend.repository.ShipmentRepository;
import com.project.backend.entity.OrderStatusHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final LocationRepository locationRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final ShippingFactory shippingFactory;
    private final WarehouseRepository warehouseRepository;
    private final CartService cartService;
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ShiprocketService shiprocketService;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final ShipmentRepository shipmentRepository;
    private final com.project.backend.repository.ProductImageRepository productImageRepository;
    private final AdminReturnService refundSevice;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ShippingAsyncService shippingAsyncService;
    private final EmailService emailService;

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

            Optional<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.get().getAvailableQuantity() - inventories.get().getReservedQuantity()
                    ;

            boolean inStock = totalAvailable >= qty;
            if (!inStock) {
                isValidForCheckout = false;
                validationErrors.add(String.format("%s is out of stock. Only %d available, you have %d in cart",
                        variant.getSku(), totalAvailable, qty));
            }

            BigDecimal price = variant.getSellingPrice();
            BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));

            // Tax is included in price — no separate tax line
            subtotal = subtotal.add(itemSubtotal);

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

        BigDecimal grandTotal = subtotal.add(shipping);
        LocalDate expectedDelivery = LocalDate.now().plusDays(maxDeliveryDays);

        CheckoutResponseDto.AddressDto addressDto = CheckoutResponseDto.AddressDto.builder()

                .addressLine1(address.getAddressLine1()).addressLine2(address.getAddressLine2()).city(address.getCity())
                .state(address.getState()).postalCode(address.getPostalCode()).country(address.getCountry()).build();

        return CheckoutResponseDto.builder().subtotal(subtotal).taxAmount(BigDecimal.ZERO).shippingCharges(shipping)
                .discountAmount(BigDecimal.ZERO).totalAmount(grandTotal).items(itemDtos)
                .totalItems(cart.getTotalQuantity()).deliveryAddress(addressDto).deliveryDays(maxDeliveryDays)
                .expectedDelivery(expectedDelivery).isDeliveryAvailable(true).paymentMethod(request.getPaymentMethod())
                .requiresPayment(request.getPaymentMethod() != PaymentMethod.COD).paymentMessage(null)
                .isCodAvailable(isCodAvailable)
                .cartId(cart.getId()).isValidForCheckout(isValidForCheckout).validationErrors(validationErrors).build();
    }

    @Transactional
    public CheckoutResponseDto buyNow(User user, BuyNowRequestDto request) {

        log.info("Buy now request for user: {}, product: {}, variant: {}, quantity: {}",
                user.getId(), request.getProductId(), request.getVariantId(), request.getQuantity());

        try {
            UserAddress address = userAddressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new NotFoundException("Address not found"));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Address does not belong to user");
            }

            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Variant not found"));

            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BadRequestException("Variant does not belong to this product");
            }

            Integer quantity = request.getQuantity();
            Optional<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.get().getAvailableQuantity() - inventories.get().getReservedQuantity()
                ;

            boolean inStock = totalAvailable >= quantity;
            List<String> validationErrors = new ArrayList<>();

            if (!inStock) {
                validationErrors.add(String.format("%s is out of stock. Only %d available, you requested %d",
                        variant.getSku(), totalAvailable, quantity));
            }

            BigDecimal price = variant.getSellingPrice();
            BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity));

            // Tax is included in price — no separate tax
            BigDecimal taxAmount = BigDecimal.ZERO;

            BigDecimal shipping;
            if (itemSubtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
                shipping = BigDecimal.ZERO;
            } else {
                shipping = BigDecimal.valueOf(50);
            }

            BigDecimal discountAmount = BigDecimal.ZERO;

            BigDecimal grandTotal = itemSubtotal.add(shipping).subtract(discountAmount);

            Integer deliveryDays = product.getDeliveryDays();
            if (deliveryDays == null) {
                deliveryDays = 7;
            }
            LocalDate expectedDelivery = LocalDate.now().plusDays(deliveryDays);

            Boolean isCodAvailable = product.getCodAvailable();
            if (isCodAvailable == null) {
                isCodAvailable = true;
            }

            int discountPercentage = 0;
            if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
                discountPercentage = mrp.subtract(price).multiply(BigDecimal.valueOf(100))
                        .divide(mrp, 0, RoundingMode.HALF_UP).intValue();
            }

            Cart tempCart = Cart.builder()
                    .user(user)
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            tempCart = cartRepository.save(tempCart);

            CartItem cartItem = CartItem.builder()
                    .cart(tempCart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .price(variant.getSellingPrice())
                    .build();
            cartItemRepository.save(cartItem);
            tempCart.setTotalQuantity(request.getQuantity());
            tempCart.setTotalAmount(variant.getSellingPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            cartRepository.save(tempCart);

            CheckoutResponseDto.CheckoutItemDto itemDto = CheckoutResponseDto.CheckoutItemDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(getProductImage(product))
                    .variantId(variant.getId())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .sku(variant.getSku())
                    .quantity(quantity)
                    .price(price)
                    .mrp(mrp)
                    .discountPercentage(discountPercentage)
                    .subtotal(itemSubtotal)
                    .inStock(inStock)
                    .availableStock(totalAvailable)
                    .build();

            CheckoutResponseDto.AddressDto addressDto = CheckoutResponseDto.AddressDto.builder()
                    .addressLine1(address.getAddressLine1())
                    .addressLine2(address.getAddressLine2())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .build();

            return CheckoutResponseDto.builder()
                    .subtotal(itemSubtotal)
                    .taxAmount(taxAmount)
                    .shippingCharges(shipping)
                    .discountAmount(discountAmount)
                    .totalAmount(grandTotal)
                    .items(List.of(itemDto))
                    .totalItems(quantity)
                    .deliveryAddress(addressDto)
                    .deliveryDays(deliveryDays)
                    .expectedDelivery(expectedDelivery)
                    .isDeliveryAvailable(true)
                    .paymentMethod(request.getPaymentMethod())
                    .requiresPayment(request.getPaymentMethod() != PaymentMethod.COD)
                    .paymentMessage(null)
                    .isCodAvailable(isCodAvailable)
                    .cartId(tempCart.getId())
                    .isValidForCheckout(inStock)
                    .validationErrors(validationErrors)
                    .build();

        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Buy now failed for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to process buy now request", e);
        }
    }

    private BigDecimal calculateGST(BigDecimal amount, ProductVariant variant) {
        BigDecimal gstPercent = getGSTPercentage(variant);
        return amount.multiply(gstPercent).divide(BigDecimal.valueOf(100));
    }

    private BigDecimal getGSTPercentage(ProductVariant variant) {
        return BigDecimal.valueOf(5);
    }

    private String getProductImage(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0).getImageUrl();
        }
        return null;
    }

    @Transactional
	public PlaceOrderResponseDto placeOrder(User user, Long addressId, PaymentMethod paymentMethod, String idempotencyKey) {
	   
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

	      


 Optional<IdempotencyKey> existingKey =
            idempotencyRepository.findByIdempotencyKey(idempotencyKey);

    if (existingKey.isPresent()) {

        IdempotencyKey key = existingKey.get();
        
  if (!key.getUserId().equals(user.getId())) {
            throw new RuntimeException("Invalid idempotency key");
        }

        if ("SUCCESS".equals(key.getStatus())) {
            Order order = orderRepository.findById(key.getOrderId())
                    .orElseThrow();
            return buildResponse(order, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0, address);
        }

        if ("PROCESSING".equals(key.getStatus())) {
            throw new RuntimeException("Request already in progress");
        }
    }
    
 IdempotencyKey newKey = new IdempotencyKey();
    newKey.setIdempotencyKey(idempotencyKey);
    newKey.setUserId(user.getId());
    newKey.setStatus("PROCESSING");
    idempotencyRepository.save(newKey);



  Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
	                .orElseThrow(() -> new BadRequestException("Cart is empty"));

 Optional<Order> existingOrder =
            orderRepository.findByCartId(cart.getId());

    if (existingOrder.isPresent()) {

        newKey.setOrderId(existingOrder.get().getId());
        newKey.setStatus("SUCCESS");
        idempotencyRepository.save(newKey);

         return buildResponse(existingOrder.get(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, address);
  
    }
    if (cart.getItems() == null || cart.getItems().isEmpty()) {
        throw new BadRequestException("Cart is empty");
    }
           
 


	        if (cart.getItems() == null || cart.getItems().isEmpty()) {
	            throw new BadRequestException("Cart has no items");
	        }

	        Warehouse defaultWarehouse = warehouseRepository.findById(1L)
	                .orElseThrow(() -> new NotFoundException("Default warehouse not configured. Please contact support."));

	        BigDecimal subtotal = BigDecimal.ZERO;
	        BigDecimal taxTotal = BigDecimal.ZERO;
	        BigDecimal shipping = BigDecimal.valueOf(12);
	        int maxDeliveryDays = 0;

try {
    ServiceabilityResponse serviceability =
            shippingFactory.getProvider(ShippingProviderType.SHIPROCKET)
                    .checkServiceability(
                            "110001",
                            address.getPostalCode(),
                            0.5,
                            paymentMethod == PaymentMethod.COD
                    );

    if (!serviceability.isServiceable()) {
        throw new BadRequestException("Delivery not available for this pincode");
    }
} catch (BadRequestException e) {
    throw e;
} catch (Exception e) {
    log.warn("Shiprocket serviceability check failed, proceeding with order: {}", e.getMessage());
}


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
	                        OrderStatus.PROCESSING : OrderStatus.PENDING_PAYMENT)
	                .paymentStatus(PaymentStatus.PENDING)
	                .paymentExpiry(paymentMethod == PaymentMethod.PREPAID ? 
	                               LocalDateTime.now().plusMinutes(15) : null)
	                .taxAmount(0.0)
	                .warehouse(defaultWarehouse)
	                .shippingCharges(shipping.doubleValue())
	                .totalAmount(0.0)
	                .build();

	        order = orderRepository.save(order);

			  List<OrderItem> savedOrderItems = new ArrayList<>();
  for (CartItem item : cart.getItems()) {

        ProductVariant variant = Optional.ofNullable(item.getVariant())
                .orElseThrow(() -> new BadRequestException("Invalid cart item"));

        Product product = Optional.ofNullable(item.getProduct())
                .orElseThrow(() -> new BadRequestException("Invalid product"));

        int quantity = item.getQuantity();

        int deliveryDays = Optional.ofNullable(product.getDeliveryDays()).orElse(5);
        maxDeliveryDays = Math.max(maxDeliveryDays, deliveryDays);

        List<WarehouseInventory> inventories =
                inventoryRepository.findByVariantIdForUpdate(variant.getId());

        int available = inventories.stream()
                .mapToInt(i -> Math.max(i.getAvailableQuantity() - i.getReservedQuantity(), 0))
                .sum();

        if (available < quantity) {
            throw new BadRequestException("Insufficient stock for " + variant.getSku());
        }

        BigDecimal price = Optional.ofNullable(variant.getSellingPrice())
                .orElseThrow(() -> new BadRequestException("Price not configured"));

        BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity));
        // Tax is included in price — no separate tax line
        subtotal = subtotal.add(itemSubtotal);

     OrderItem orderItem =   orderItemRepository.save(OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .variantId(variant.getId())
                .productName(product.getName())
                .price(price.doubleValue())
                .quantity(quantity)
                .size(variant.getSize())
                .color(variant.getColor())
                .status(paymentMethod == PaymentMethod.COD ? OrderStatus.PROCESSING : OrderStatus.PENDING_PAYMENT)
                .build());
   savedOrderItems.add(orderItem); 
        reserveStock(inventories, quantity);
    }


	       if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
        shipping = BigDecimal.ZERO;
    }

    // Tax included in price — total = subtotal + shipping
    BigDecimal total = subtotal.add(shipping);

    order.setTaxAmount(0.0);
    order.setShippingCharges(shipping.doubleValue());
    order.setTotalAmount(total.doubleValue());

    orderRepository.save(order);

    // Auto-record PLACED + PROCESSING history for COD (payment already confirmed by intent)
    if (paymentMethod == PaymentMethod.COD) {
        saveInitialStatusHistory(order);
    }

	    try {
        cart.getItems().clear();
        cart.setStatus("COMPLETED"); 
        cartRepository.save(cart);

         newKey.setOrderId(order.getId());
    newKey.setCartId(cart.getId());
    newKey.setStatus("SUCCESS");
    idempotencyRepository.save(newKey);
    } catch (Exception e) {
        log.warn("Cart cleanup failed for orderId={}", order.getId());
    }
 order.setItems(savedOrderItems);

        // Send order confirmation email (async, fire-and-forget)
        emailService.sendOrderConfirmation(user, order, savedOrderItems);

        // For COD orders, trigger shipping immediately.
        // For Prepaid, shipping is triggered after payment verification (see verifyPayment).
        if (paymentMethod == PaymentMethod.COD) {
            triggerShippingAsync(order.getId());
        }

    return buildResponse(order, subtotal, taxTotal, shipping, maxDeliveryDays, address);
	}

    public void triggerShippingAsync(Long orderId) {
        shippingAsyncService.triggerShippingAsync(orderId);
    }

    private void updateOrderWithShipment(Order order, ShipmentResponse shipment) {

        order.setShippingProvider(ShippingProviderType.SHIPROCKET.name());
        order.setShipmentId(shipment.getShipmentId());
      
        order.setShippingStatus("CREATED");

        orderRepository.save(order);
log.info("Shipment response: {}", shipment);
        log.info("Shipment created for orderId={}", order.getId());
    }

    private PlaceOrderResponseDto buildResponse(
            Order order,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal shipping,
            int deliveryDays,
            UserAddress address) {

        return PlaceOrderResponseDto.builder()
                .orderId(order.getId())
                .subtotal(subtotal)
                .taxAmount(tax)
                .shippingCharges(shipping)
                .totalAmount(BigDecimal.valueOf(order.getTotalAmount()))
                .orderStatus(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .expectedDelivery(LocalDate.now().plusDays(deliveryDays))
                .deliveryDays(deliveryDays)
                .paymentExpiry(order.getPaymentExpiry())
                .requiresPayment(order.getPaymentMethod() != PaymentMethod.COD)
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

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> loggedUserLogin(User user, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> orders = orderRepository.findByUser(user, pageable);

        Map<Long, String> imageMap = buildProductImageMap(orders.getContent());

        return PageResponseDto.<OrderResponseDto>builder()
                .content(orders.getContent().stream().map(o -> OrderMapper.toDto(o, imageMap)).toList())
                .page(orders.getNumber())
                .size(orders.getSize()).totalElements(orders.getTotalElements()).totalPages(orders.getTotalPages())
                .last(orders.isLast()).build();

    }

    private Map<Long, String> buildProductImageMap(java.util.List<Order> orders) {
        Map<Long, String> imageMap = new HashMap<>();
        orders.forEach(order -> {
            if (order.getItems() == null) return;
            order.getItems().forEach(item -> {
                if (item.getProductId() == null || imageMap.containsKey(item.getProductId())) return;
                productImageRepository.findByProductIdOrderByPositionAsc(item.getProductId())
                    .stream().findFirst()
                    .ifPresent(img -> imageMap.put(item.getProductId(), img.getImageUrl()));
            });
        });
        return imageMap;
    }

    @Transactional
public void cancelFullOrder(Long orderId, User user) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

    if (!order.getUser().getId().equals(user.getId())) {
        throw new UnauthorizedException("You cannot cancel this order");
    }

    if (order.getStatus() == OrderStatus.CANCELLED) {
        log.info("Order already cancelled {}", orderId);
        return;
    }

    if (order.getStatus() == OrderStatus.SHIPPED ||
        order.getStatus() == OrderStatus.DELIVERED) {
        throw new BadRequestException("Order cannot be cancelled at this stage");
    }

    if (order.getShipmentId() != null) {
        try {
            shiprocketService.cancelShipment(order.getId()+"");
        } catch (Exception ex) {
            log.error("Shiprocket cancel failed {}", order.getShipmentId(), ex);
            throw new BadRequestException("Unable to cancel shipment");
        }
    }

    for (OrderItem item : order.getItems()) {

        if (item.getStatus() == OrderStatus.CANCELLED) continue;

        item.setStatus(OrderStatus.CANCELLED);
       // item.setCancelledAt(Instant.now());

        WarehouseInventory inventory = inventoryRepository
                .findByVariantId(item.getVariantId())
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - item.getQuantity()
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + item.getQuantity()
        );

        // if (order.getPaymentStatus() == PaymentStatus.PAID) {
        //     refundService.initiateRefund(order, item);
        // }
    }

    order.setStatus(OrderStatus.CANCELLED);

    if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
    }

    log.info("Full order cancelled {}", orderId);

    // Send cancellation email asynchronously
    try {
        emailService.sendOrderCancellationEmail(user, order);
    } catch (Exception e) {
        log.warn("Failed to send cancellation email for orderId={}: {}", orderId, e.getMessage());
    }
}
@Transactional
public void cancelOrderItems(Long orderId, List<Long> itemIds, User user) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));

    if (!order.getUser().getId().equals(user.getId())) {
        throw new UnauthorizedException("Unauthorized");
    }

    List<OrderItem> items = order.getItems().stream()
            .filter(i -> itemIds.contains(i.getId()))
            .toList();

    if (items.isEmpty()) {
        throw new BadRequestException("No valid items");
    }

    for (OrderItem item : items) {

        if (item.getStatus() == OrderStatus.CANCELLED) continue;

        if (item.getStatus() == OrderStatus.SHIPPED ||
            item.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Item already shipped");
        }

        item.setStatus(OrderStatus.CANCELLED);
      

        WarehouseInventory inventory =
                inventoryRepository.findByVariantId(item.getVariantId())
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - item.getQuantity()
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + item.getQuantity()
        );

   

        // if (order.getPaymentStatus() == PaymentStatus.PAID) {
        //     refundService.processRefund(order, item);
        // }
    }

    boolean allCancelled = order.getItems().stream()
            .allMatch(i -> i.getStatus() == OrderStatus.CANCELLED);

    if (allCancelled) {
        order.setStatus(OrderStatus.CANCELLED);
    } else {
        order.setStatus(OrderStatus.PARTIALLY_CANCELLED);
    }

    // Send cancellation/partial-cancellation email asynchronously
    try {
        emailService.sendOrderCancellationEmail(user, order);
    } catch (Exception e) {
        log.warn("Failed to send cancellation email for orderId={}: {}", orderId, e.getMessage());
    }
}
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId, User user) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not your order");
        }

        Map<Long, String> imageMap = buildProductImageMap(java.util.List.of(order));
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrder_IdOrderByChangedAtAsc(orderId);
        return OrderMapper.toDto(order, imageMap, history);
    }

    @Transactional
    public void reorder(User user, Long orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not your order");
        }

        for (OrderItem item : order.getItems()) {
            // cartService.addOrUpdate(user, item.getProduct(), item.getQuantity());

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

    @Transactional(readOnly = true)
    public BuyNowCheckoutResponseDto buyNowCheckout(User user, BuyNowRequestDto request) {

        log.info("Buy now checkout for user: {}, product: {}, quantity: {}",
                user.getId(), request.getProductId(), request.getQuantity());

        try {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Variant not found"));

            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BadRequestException("Variant does not belong to this product");
            }

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException(product.getName() + " is not available");
            }

            Integer quantity = request.getQuantity();
           Optional< WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.get().getAvailableQuantity() - inventories.get().getReservedQuantity();

            boolean inStock = totalAvailable >= quantity;
            List<String> validationErrors = new ArrayList<>();

            if (!inStock) {
                validationErrors.add(String.format("Only %d available, you requested %d", totalAvailable, quantity));
            }

            UserAddress address = userAddressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new NotFoundException("Address not found"));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Address does not belong to user");
            }

            BigDecimal price = variant.getSellingPrice();
            BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

            int discountPercentage = 0;
            if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
                discountPercentage = mrp.subtract(price)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(mrp, 0, RoundingMode.HALF_UP)
                        .intValue();
            }

            // Tax is included in price — no separate tax
            BigDecimal tax = BigDecimal.ZERO;

            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(999)) > 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(50);

            BigDecimal total = subtotal.add(shipping);

            Integer deliveryDays = product.getDeliveryDays();
            if (deliveryDays == null)
                deliveryDays = 7;
            LocalDate expectedDelivery = LocalDate.now().plusDays(deliveryDays);

            Boolean isCodAvailable = product.getCodAvailable() != null ? product.getCodAvailable() : true;

            BuyNowCheckoutResponseDto.AddressDto addressDto = BuyNowCheckoutResponseDto.AddressDto.builder()
                    .addressId(address.getId())
                    .addressLine1(address.getAddressLine1())
                    .addressLine2(address.getAddressLine2())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .build();

            String productImage = getProductImage(product);

            return BuyNowCheckoutResponseDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(productImage)
                    .variantId(variant.getId())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(quantity)
                    .price(price)
                    .mrp(mrp)
                    .discountPercentage(discountPercentage)
                    .subtotal(subtotal)
                    .deliveryAddress(addressDto)
                    .deliveryDays(deliveryDays)
                    .expectedDelivery(expectedDelivery)
                    .taxAmount(tax)
                    .shippingCharges(shipping)

                    .totalAmount(total)
                    .paymentMethod(request.getPaymentMethod())
                    .requiresPayment(request.getPaymentMethod() != PaymentMethod.COD)
                    .isCodAvailable(isCodAvailable)
                    .validationErrors(validationErrors)
                    .isValid(inStock)
                    .build();

        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Buy now checkout failed for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to process buy now checkout", e);
        }
    }

    @Transactional
    public OrderResponseDto buyNowPlaceOrder(User user, BuyNowRequestDto request,String idempotencyKey) {

        log.info("Buy now place order for user: {}, product: {}, quantity: {}",
                user.getId(), request.getProductId(), request.getQuantity());

        try {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));

            ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Variant not found"));

            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BadRequestException("Variant does not belong to this product");
            }

   UserAddress address = userAddressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new NotFoundException("Address not found"));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Address does not belong to user");
            }



 Optional<IdempotencyKey> existingKey =
            idempotencyRepository.findByIdempotencyKey(idempotencyKey);

    if (existingKey.isPresent()) {

    IdempotencyKey key = existingKey.get();

    if (!key.getUserId().equals(user.getId())) {
        throw new RuntimeException("Invalid idempotency key");
    }

    if ("SUCCESS".equals(key.getStatus())) {
        Order order = orderRepository.findById(key.getOrderId()).orElseThrow();
        return OrderMapper.toDto(order);
    }

    if ("PROCESSING".equals(key.getStatus())) {
        throw new RuntimeException("Request already in progress");
    }
}
    
 IdempotencyKey newKey = new IdempotencyKey();
    newKey.setIdempotencyKey(idempotencyKey);
    newKey.setUserId(user.getId());
    newKey.setStatus("PROCESSING");
    idempotencyRepository.save(newKey);




            Warehouse defaultWarehouse = warehouseRepository.findById(1L)
                    .orElseThrow(
                            () -> new NotFoundException("Default warehouse not configured. Please contact support."));

            Integer quantity = request.getQuantity();
            Optional<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.get().getAvailableQuantity() - inventories.get().getReservedQuantity()
                 ;

            if (totalAvailable < quantity) {
                throw new BadRequestException(String.format(
                        "Insufficient stock. Only %d available, you requested %d", totalAvailable, quantity));
            }

         
            BigDecimal price = variant.getSellingPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            // Tax is included in price — no separate tax
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(999)) > 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(50);

            BigDecimal total = subtotal.add(shipping);

            Order order = Order.builder()
                    .user(user)
                    .shippingAddressId(address.getId())
                    .deliveryAddressLine1(address.getAddressLine1())
                    .deliveryAddressLine2(address.getAddressLine2())
                    .deliveryCity(address.getCity())
                    .deliveryState(address.getState())
                    .deliveryPostalCode(address.getPostalCode())
                    .deliveryCountry(address.getCountry())
                    .paymentMethod(request.getPaymentMethod())
                    .status(request.getPaymentMethod() == PaymentMethod.COD ? OrderStatus.PROCESSING
                            : OrderStatus.PENDING_PAYMENT)
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentExpiry(request.getPaymentMethod() != PaymentMethod.COD ? LocalDateTime.now().plusMinutes(15)
                            : null)
                    .totalAmount(total.doubleValue())
                    .taxAmount(tax.doubleValue())
                    .shippingCharges(shipping.doubleValue())
                    .shippingStatus("SHIPPING_IN_PROGRESS")
                    .warehouse(defaultWarehouse)
                    .build();

        

            order = orderRepository.save(order);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .variantId(variant.getId())
                    .productName(product.getName())
                    .price(price.doubleValue())
                    .quantity(quantity)
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .status(request.getPaymentMethod() == PaymentMethod.COD ? OrderStatus.PROCESSING : OrderStatus.PENDING_PAYMENT)
                    .build();
            orderItemRepository.save(orderItem);

            reserveStock(variant, quantity);

            log.info("Buy now order placed successfully: orderId={}", order.getId());

            // Auto-record PLACED + PROCESSING history for COD
            if (request.getPaymentMethod() == PaymentMethod.COD) {
                saveInitialStatusHistory(order);
            }

            // Send order confirmation email (async)
            emailService.sendOrderConfirmation(user, order, List.of(orderItem));

            if (request.getPaymentMethod() == PaymentMethod.COD) {
                triggerShippingAsync(order.getId());
            }
            return OrderMapper.toDto(order);

        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Buy now place order failed for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to place buy now order", e);
        }
    }

    private void reserveStock(ProductVariant variant, Integer quantity) {
        List<WarehouseInventory> inventories = inventoryRepository.findByVariantIdWithLock(variant.getId());

        int remainingToReserve = quantity;

        for (WarehouseInventory inventory : inventories) {
            if (remainingToReserve <= 0)
                break;

            int available = inventory.getAvailableQuantity() - inventory.getReservedQuantity();
            if (available > 0) {
                int reserveFromThis = Math.min(available, remainingToReserve);
                inventory.setReservedQuantity(inventory.getReservedQuantity() + reserveFromThis);
                remainingToReserve -= reserveFromThis;
            }
        }

        if (remainingToReserve > 0) {
            throw new BadRequestException("Failed to reserve stock");
        }
    }

    /**
     * Records PLACED + PROCESSING history entries when an order is auto-confirmed
     * (COD on placement, Prepaid on payment verification).
     * PLACED gets timestamp 1 minute before PROCESSING so the timeline looks correct.
     */
    private void saveInitialStatusHistory(com.project.backend.entity.Order order) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<OrderStatusHistory> entries = new java.util.ArrayList<>();

        OrderStatusHistory placed = new OrderStatusHistory();
        placed.setOrder(order);
        placed.setStatus(OrderStatus.PLACED.name());
        placed.setChangedAt(now.minusMinutes(1));
        entries.add(placed);

        OrderStatusHistory processing = new OrderStatusHistory();
        processing.setOrder(order);
        processing.setStatus(OrderStatus.PROCESSING.name());
        processing.setChangedAt(now);
        entries.add(processing);

        orderStatusHistoryRepository.saveAll(entries);
    }

    public TrackingResponseDto getTracking(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getTrackingId() == null) {
            return TrackingResponseDto.builder()
                    .orderId(orderId)
                    .status("CREATED")
                    .trackingId(null)
                    .events(List.of())
                    .build();
        }

        TrackingResponse tracking = shippingFactory.getProvider(ShippingProviderType.SHIPROCKET)
                .trackShipment(order.getTrackingId());

        return TrackingResponseDto.builder()
                .orderId(orderId)
                .status(order.getShippingStatus())
                .trackingId(order.getTrackingId())
                .events(
                        tracking.getEvents().stream()
                                .map(e -> TrackingResponseDto.TrackingEvent.builder()
                                        .status(e.getStatus())
                                        .date(e.getDate())
                                        .location(e.getLocation())
                                        .build())
                                .toList())
                .build();
    }

}
