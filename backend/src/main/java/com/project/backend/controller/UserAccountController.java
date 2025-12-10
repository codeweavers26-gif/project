package com.project.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import com.project.backend.entity.UserProfile;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.UserAddressDto;
import com.project.backend.requestDto.UserProfileDto;
import com.project.backend.service.UserAddressService;
import com.project.backend.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Manage user profile & address operations")
public class UserAccountController {

    private final UserRepository userRepository;
    private final UserProfileService profileService;
    private final UserAddressService addressService;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ---------------- PROFILE ----------------

    @Operation(summary = "Get current user profile", description = "Returns the profile of the logged-in user")
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(Authentication auth) {
        return ResponseEntity.ok(profileService.getProfile(getCurrentUser(auth)));
    }

    @Operation(summary = "Update user profile", description = "Updates the logged-in user's profile details")
    @PostMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(
            Authentication auth,
            @RequestBody UserProfileDto dto) {
        return ResponseEntity.ok(profileService.updateProfile(getCurrentUser(auth), dto));
    }

    // ---------------- ADDRESSES ----------------

    @Operation(summary = "Get all addresses of user", description = "Fetches all saved addresses of the logged-in user")
    @GetMapping("/addresses")
    public ResponseEntity<List<UserAddress>> getAddresses(Authentication auth) {
        return ResponseEntity.ok(addressService.getAllAddresses(getCurrentUser(auth)));
    }

    @Operation(summary = "Add a new address", description = "Adds a new address to the logged-in user's account")
    @PostMapping("/addresses")
    public ResponseEntity<UserAddress> addAddress(
            Authentication auth,
            @RequestBody UserAddressDto dto) {
        return ResponseEntity.ok(addressService.addAddress(getCurrentUser(auth), dto));
    }

    @Operation(summary = "Delete an address", description = "Deletes address by ID from logged-in user's address list")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
