package com.project.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.ApiResponse;
import com.project.backend.ResponseDto.CartItemResponseDto;
import com.project.backend.ResponseDto.CartPricingResponseDto;
import com.project.backend.ResponseDto.MergeCartResultDto;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.CartMergeDto;
import com.project.backend.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Operation(summary = "Add product to cart", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addToCart(
            Authentication auth,
            @RequestParam Long productId,
            @RequestParam Long variantId,
            @RequestParam Integer qty) {

        cartService.addToCart(getCurrentUser(auth), productId, variantId,qty);
        
        String message = String.format("Item added to cart successfully (Quantity: %d)", qty);
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    @Operation(summary = "Get cart items", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(getCurrentUser(auth)));
    }
    
    @Operation(summary = "Update cart item quantity/variant", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse> updateQty(
            Authentication auth,
            @PathVariable Long cartItemId,
            @RequestParam Integer qty,
            @RequestParam Integer variantId) {

        cartService.updateCartItem(getCurrentUser(auth), cartItemId, qty,variantId);
        String message = qty <= 0 ? "Item removed from cart" : "Cart item updated successfully";
        return ResponseEntity.ok(new ApiResponse(true, message));

    }

    @Operation(summary = "Remove item from cart", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse> removeItem(
            Authentication auth,
            @PathVariable Long cartItemId) {

        cartService.removeCartItem(getCurrentUser(auth), cartItemId);

        return ResponseEntity.ok(new ApiResponse(true, "Item removed from cart successfully"));
    }
    
      
    @Operation(summary = "Merge guest cart with user cart", security = {
    	    @SecurityRequirement(name = "Bearer Authentication")
    	})
    	@PostMapping("/merge")
    	public ResponseEntity<MergeCartResultDto> mergeCart(
    	        Authentication auth,
    	        @RequestBody List<CartMergeDto> items) {

    	    User user = getCurrentUser(auth);
    	    MergeCartResultDto result = cartService.mergeCart(user, items);
    	    
    	    if (result.isSuccess()) {
    	        return ResponseEntity.ok(result);
    	    } else {
    	        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);
    	    }
    	}
    @Operation(summary = "Get cart with pricing", security = {
            @SecurityRequirement(name = "Bearer Authentication") })
    @GetMapping("/summary")
    public ResponseEntity<CartPricingResponseDto> getCartSummary(
            Authentication auth,
            @RequestParam(required = false) String couponCode) {

        User user = getCurrentUser(auth);
        return ResponseEntity.ok(cartService.getCartPricing(user, couponCode));
    }

}
