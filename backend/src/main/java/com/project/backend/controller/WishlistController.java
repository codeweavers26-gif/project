package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.WishlistResponseDto;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "add wishlistt", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

    @PostMapping("/{productId}")
    public ResponseEntity<Void> add(Authentication auth, @PathVariable Long productId) {
        wishlistService.add(user(auth), productId);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "remove from wishlist", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long productId) {
        wishlistService.remove(user(auth), productId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "get all wishlist", security = {
			@SecurityRequirement(name = "Bearer Authentication") })

    @GetMapping
    public ResponseEntity<PageResponseDto<WishlistResponseDto>> get(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            wishlistService.get(user(auth), page, size)
        );
    }
}
