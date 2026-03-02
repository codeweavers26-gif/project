package com.project.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.ApiResponse;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.MoveToCartRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.WishlistItemDto;
import com.project.backend.requestDto.WishlistRequestDto;
import com.project.backend.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepo;

    private User user(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Operation(summary = "add wishlist", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

    @PostMapping()
    public ResponseEntity<ApiResponse> add(Authentication auth, @RequestBody WishlistRequestDto dto) {
        wishlistService.add(user(auth), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Item added to wishlist successfully"));

    }
    
    @Operation(summary = "remove from wishlist", security = {		@SecurityRequirement(name = "Bearer Authentication") })

    @DeleteMapping("/{wishlistItemId}")
    public ResponseEntity<ApiResponse> remove(Authentication auth, @PathVariable Long wishlistItemId) {
        wishlistService.removeById(user(auth), wishlistItemId);
        return ResponseEntity.ok(new ApiResponse(true, "Item removed from wishlist successfully"));

    }
    
    @Operation(summary = "get all wishlist", security = {
			@SecurityRequirement(name = "Bearer Authentication") })
    @GetMapping
    public ResponseEntity<PageResponseDto<WishlistItemDto>> get(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            wishlistService.get(user(auth), page, size)
        );
    }
    
    
    
    @Operation(summary = "Move multiple items to cart", security = {
    	    @SecurityRequirement(name = "Bearer Authentication")
    	})
    	@PostMapping("/move-to-cart")
    	public ResponseEntity<ApiResponse> moveToCart(
    	        Authentication auth,
    	        @Valid @RequestBody MoveToCartRequestDto request) {
    	    
    	    User user = user(auth);
    	    
    	    wishlistService.moveToCart(user, request);
    	    String message = request.getItemIds().size() == 1 
    	            ? "Item moved to cart successfully" 
    	            : request.getItemIds().size() + " items moved to cart successfully";
    	        
    	        return ResponseEntity.ok(new ApiResponse(true, message));
    	}


    	@Operation(summary = "Clear entire wishlist", security = {
    	    @SecurityRequirement(name = "Bearer Authentication")
    	})
    	@DeleteMapping("/clear")
    	public ResponseEntity<ApiResponse> clearWishlist(Authentication auth) {
    	    User user = user(auth);
    	    
    	    wishlistService.clearWishlist(user);
    	    return ResponseEntity.ok(new ApiResponse(true, "Wishlist cleared successfully"));
    	}
}
