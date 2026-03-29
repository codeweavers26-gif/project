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
import com.project.backend.repository.ShipmentRepository;

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
            List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.stream()
                    .mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
                    .sum();

            boolean inStock = totalAvailable >= quantity;
            List<String> validationErrors = new ArrayList<>();

            if (!inStock) {
                validationErrors.add(String.format("%s is out of stock. Only %d available, you requested %d",
                        variant.getSku(), totalAvailable, quantity));
            }

            BigDecimal price = variant.getSellingPrice();
            BigDecimal mrp = variant.getMrp() != null ? variant.getMrp() : price;
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(quantity));

            BigDecimal gstPercent = BigDecimal.valueOf(5);
            BigDecimal taxAmount = itemSubtotal.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);

            BigDecimal shipping;
            if (itemSubtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
                shipping = BigDecimal.ZERO;
            } else {
                shipping = BigDecimal.valueOf(50);
            }

            BigDecimal discountAmount = BigDecimal.ZERO;

            BigDecimal grandTotal = itemSubtotal.add(taxAmount).add(shipping).subtract(discountAmount);

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
        BigDecimal gstPercent = BigDecimal.valueOf(
                Optional.ofNullable(product.getTaxPercent()).orElse(5.0)
        );

        BigDecimal itemTax = itemSubtotal.multiply(gstPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        subtotal = subtotal.add(itemSubtotal);
        taxTotal = taxTotal.add(itemTax);

     OrderItem orderItem =   orderItemRepository.save(OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .variantId(variant.getId())
                .productName(product.getName())
                .price(price.doubleValue())
                .quantity(quantity)
                .size(variant.getSize())
                .color(variant.getColor())
                .build());
   savedOrderItems.add(orderItem); 
        reserveStock(inventories, quantity);
    }


	       if (subtotal.compareTo(BigDecimal.valueOf(999)) > 0) {
        shipping = BigDecimal.ZERO;
    }

    BigDecimal total = subtotal.add(taxTotal).add(shipping);

    order.setTaxAmount(taxTotal.doubleValue());
    order.setShippingCharges(shipping.doubleValue());
    order.setTotalAmount(total.doubleValue());

    orderRepository.save(order);

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
    

	        triggerShippingAsync((order.getId()));

    return buildResponse(order, subtotal, taxTotal, shipping, maxDeliveryDays, address);
	}

    @Async
    public void triggerShippingAsync(Long orderId) {
 Order order = orderRepository.findById(orderId).orElseThrow();
        log.info("Triggering shipping for orderId={}", order.getId());

        try {

            ShipmentResponse shipment = RetryUtil.executeWithRetry(
                    () -> shippingFactory
                            .getProvider(ShippingProviderType.SHIPROCKET)
                            .createShipment(order),
                    3);

            updateOrderWithShipment(order, shipment);

            ShipmentResponse assigned = shiprocketService.assignCourier(shipment.getShipmentId());

log.info("Assigned courier response: {}", assigned);
            order.setTrackingId(assigned.getTrackingId());
            order.setShippingProvider("SHIPROCKET");
            order.setShippingStatus("AWB_ASSIGNED");

            orderRepository.save(order);
                


              Shipment shipmentEntity = Shipment.builder()
                .order(order)
                .trackingId(assigned.getTrackingId())
                .shippingStatus("AWB_ASSIGNED")
                .courierName("SHIPROCKET")
                .warehouse(order.getWarehouse())
                .build();

        shipmentRepository.save(shipmentEntity);

        } catch (Exception e) {
            log.error("Shiprocket failed for orderId={}", order.getId(), e);

            order.setShippingStatus("FAILED");
            orderRepository.save(order);
        }
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
            List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.stream()
                    .mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
                    .sum();

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

            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(5))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(999)) > 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(50);

            BigDecimal total = subtotal.add(tax).add(shipping);

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
            List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());

            int totalAvailable = inventories.stream()
                    .mapToInt(i -> i.getAvailableQuantity() - i.getReservedQuantity())
                    .sum();

            if (totalAvailable < quantity) {
                throw new BadRequestException(String.format(
                        "Insufficient stock. Only %d available, you requested %d", totalAvailable, quantity));
            }

         
            BigDecimal price = variant.getSellingPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(5))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(999)) > 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(50);

            BigDecimal total = subtotal.add(tax).add(shipping);

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
                    .status(request.getPaymentMethod() == PaymentMethod.COD ? OrderStatus.PENDING
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
                    .build();

            orderItemRepository.save(orderItem);

            reserveStock(variant, quantity);

            log.info("Buy now order placed successfully: orderId={}", order.getId());

            triggerShippingAsync(order.getId());
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
