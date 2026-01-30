package com.project.backend.controller;

import java.util.List;

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

import com.project.backend.ResponseDto.CartItemResponseDto;
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

    // ADD TO CART
    @Operation(summary = "Add product to cart", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(
            Authentication auth,
            @RequestParam Long productId,
            @RequestParam Integer qty) {

        cartService.addToCart(getCurrentUser(auth), productId, qty);
        return ResponseEntity.ok().build();
    }

    // VIEW CART
    @Operation(summary = "Get cart items", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @GetMapping
    public ResponseEntity<List<CartItemResponseDto>> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(getCurrentUser(auth)));
    }

    // UPDATE QUANTITY
    @Operation(summary = "Update cart item quantity", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @PutMapping("/{cartId}")
    public ResponseEntity<Void> updateQty(
            Authentication auth,
            @PathVariable Long cartId,
            @RequestParam Integer qty) {

        cartService.updateQuantity(getCurrentUser(auth), cartId, qty);
        return ResponseEntity.ok().build();
    }

    // REMOVE ITEM
    @Operation(summary = "Remove item from cart", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> removeItem(
            Authentication auth,
            @PathVariable Long cartId) {

        cartService.removeItem(getCurrentUser(auth), cartId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/merge")
    public ResponseEntity<Void> mergeCart(
            Authentication auth,
            @RequestBody List<CartMergeDto> items) {

        User user = getCurrentUser(auth);
        cartService.mergeCart(user, items);
        return ResponseEntity.ok().build();
    }

}
