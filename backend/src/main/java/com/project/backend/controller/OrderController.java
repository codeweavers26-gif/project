package com.project.backend.controller;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.project.backend.ResponseDto.CheckoutResponseDto;
import com.project.backend.ResponseDto.CreateOrderResponse;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.PaymentResponse;
import com.project.backend.ResponseDto.PlaceOrderResponseDto;
import com.project.backend.ResponseDto.TrackingResponse;
import com.project.backend.ResponseDto.TrackingResponseDto;
import com.project.backend.config.ShippingFactory;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.ShippingProviderType;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.BuyNowCheckoutResponseDto;
import com.project.backend.requestDto.BuyNowRequestDto;
import com.project.backend.requestDto.CheckoutRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.PaymentRequest;
import com.project.backend.requestDto.VerifyPaymentRequest;
import com.project.backend.service.OrderService;
import com.project.backend.service.RazorpayService;
import com.project.backend.service.ShiprocketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

	private final OrderService orderService;
	private final UserRepository userRepository;
	private final RazorpayService razorpayService;
	private final ShiprocketService shiprocketService;
	 private final RestTemplate restTemplate; 
	private final ShippingFactory shippingFactory;


	private User getCurrentUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Operation(summary = "Checkout (login required)", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/checkout")
	public ResponseEntity<CheckoutResponseDto> checkout(Authentication auth,
			@Valid @RequestBody CheckoutRequestDto request) {

		User user = getCurrentUser(auth);
		CheckoutResponseDto order = orderService.checkout(user, request);
		return ResponseEntity.ok(order);
	}



 @Operation(summary = "Buy Now - Get checkout details", security = {
            @SecurityRequirement(name = "Bearer Authentication") })
    @PostMapping("/buy-now/checkout")
    public ResponseEntity<BuyNowCheckoutResponseDto> buyNowCheckout(
            Authentication auth,
            @Valid @RequestBody BuyNowRequestDto request) {
        
        User user = getCurrentUser(auth);
        BuyNowCheckoutResponseDto response = orderService.buyNowCheckout(user, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buy Now - Place order", security = {
            @SecurityRequirement(name = "Bearer Authentication") })
    @PostMapping("/buy-now/place-order")
    public ResponseEntity<OrderResponseDto> buyNowPlaceOrder( @RequestHeader("Idempotency-Key") String key,
            Authentication auth,
            @Valid @RequestBody BuyNowRequestDto request) {
        
        User user = getCurrentUser(auth);
        OrderResponseDto response = orderService.buyNowPlaceOrder(user, request,key);
        return ResponseEntity.ok(response);
    }

	@Operation(summary = "Get logged-in user's orders", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	@GetMapping("/my-orders")
	public ResponseEntity<PageResponseDto<OrderResponseDto>> myOrders(Authentication auth,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		User user = getCurrentUser(auth);

		return ResponseEntity.ok(orderService.loggedUserLogin(user, page, size));

	}

	@Operation(summary = "cancel order", security = { @SecurityRequirement(name = "Bearer Authentication") })
	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication auth) {

		User user = getCurrentUser(auth);
		orderService.cancelOrder(orderId, user);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{orderId}")
	@Operation(summary = "Get order details", security = { @SecurityRequirement(name = "Bearer Authentication") })
	public ResponseEntity<OrderResponseDto> getOrder(Authentication auth, @PathVariable Long orderId) {

		User user = getCurrentUser(auth);
		return ResponseEntity.ok(orderService.getOrderById(orderId, user));
	}

	@PostMapping("/{orderId}/payment/initiate")
	@Operation(summary = "Initiate payment for prepaid order", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
	public ResponseEntity<CreateOrderResponse> initiatePayment(Authentication auth,
	        @PathVariable Long orderId,
	        @RequestBody PaymentRequest paymentRequest) {
		User user = getCurrentUser(auth);
	    paymentRequest.setOrderId(orderId);
	    
	    return ResponseEntity.ok(razorpayService.createOrder(paymentRequest, user));
	}
	
	   @PostMapping("/verify")

		@Operation(summary = "Get order details", security = { @SecurityRequirement(name = "Bearer Authentication") })
	    public ResponseEntity<Map<String, Object>> verifyPayment(Authentication auth,@RequestBody VerifyPaymentRequest request) {
		   
		   User user = getCurrentUser(auth);
		   PaymentResponse res = razorpayService.verifyPayment(request,user);
	        
		   
	        boolean isValid =res.isSuccess();
	        		
	        		
	        
	        if (isValid) {
	            return ResponseEntity.ok(Map.of(
	                "success", true,
	                "message", "Payment verified successfully"
	            ));
	        } else {
	            return ResponseEntity.badRequest().body(Map.of(
	                "success", false,
	                "message", "Payment verification failed"
	            ));
	        }
	    }

	    @PostMapping("/webhook")

		@Operation(summary = "Get order details", security = { @SecurityRequirement(name = "Bearer Authentication") })
	    public ResponseEntity<String> handleWebhook(
	            @RequestBody String payload,
	            @RequestHeader("X-Razorpay-Signature") String signature) {
	        
	    	razorpayService.handleWebhook(payload, signature);
	        return ResponseEntity.ok("Webhook received");
	    }
	
	 @PostMapping("/place")
	 @Operation(summary = "place order", security = {
				@SecurityRequirement(name = "Bearer Authentication") })
	    public ResponseEntity<PlaceOrderResponseDto> placeOrder(  @RequestHeader("Idempotency-Key") String key,
	    		Authentication auth ,
	    		@RequestParam Long addressId, 	@RequestParam PaymentMethod paymentMethod
	         ) {
		 User user = getCurrentUser(auth);
		 PlaceOrderResponseDto response = orderService.placeOrder(user, addressId,paymentMethod,key );

	        return ResponseEntity.ok(response);
	    }
@GetMapping("/track/{trackingId}")
@Operation(summary = "place order", security = {
				@SecurityRequirement(name = "Bearer Authentication") })
public TrackingResponse trackShipment(@PathVariable String trackingId) {

    return shippingFactory
            .getProvider(ShippingProviderType.SHIPROCKET)
            .trackShipment(trackingId);
}


@GetMapping("/shiprocket/pickup-locations")
@Operation(summary = "place order", security = {
				@SecurityRequirement(name = "Bearer Authentication") })
public ResponseEntity<?> getPickupLocations() {
    try {
        String token = shiprocketService.getValidToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://apiv2.shiprocket.in/v1/external/settings/company/pickup",
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        return ResponseEntity.ok(response.getBody());
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
}
	

// @PostMapping("/webhook/shiprocket")
// public ResponseEntity<Void> handleShiprocketWebhook(
//         @RequestBody Map<String, Object> payload,
//         @RequestParam String secret) {

//     if (!"abc123".equals(secret)) {
//         throw new RuntimeException("Unauthorized webhook");
//     }

//     shiprocketService.handleWebhook(payload);

//     return ResponseEntity.ok().build();
// }


@GetMapping("/orders/{orderId}/tracking")
@Operation(summary = "place order", security = {
				@SecurityRequirement(name = "Bearer Authentication") })
public ResponseEntity<TrackingResponseDto> getTracking(@PathVariable Long orderId) {

    return ResponseEntity.ok(orderService.getTracking(orderId));
}











}
