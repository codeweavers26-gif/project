package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.project.backend.ResponseDto.AuthResponse;
import com.project.backend.ResponseDto.MessageResponse;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.AuthRequest;
import com.project.backend.requestDto.RegisterRequest;
import com.project.backend.requestDto.TokenRefreshRequest;
import com.project.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Handles login, register, refresh token, logout")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // ==========================
    // REGISTER
    // ==========================
    @Operation(summary = "Register a new account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Validated @RequestBody RegisterRequest req) {

        AuthResponse resp = authService.register(req);
        return ResponseEntity.ok(resp);
    }

    // ==========================
    // LOGIN
    // ==========================
    @Operation(summary = "Login user and generate JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Validated @RequestBody AuthRequest req) {

        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }

    // ==========================
    // REFRESH TOKEN
    // ==========================
    @Operation(summary = "Refresh JWT access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Validated @RequestBody TokenRefreshRequest request) {

        AuthResponse resp = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(resp);
    }

    // ==========================
    // LOGOUT
    // ==========================
    @Operation(summary = "Logout user (invalidate refresh tokens)")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(Authentication authentication) {

        if (authentication == null) {
            // Idempotent logout
            return ResponseEntity.ok(
                new MessageResponse("Logged out successfully")
            );
        }

        authService.logout(authentication.getName());
        return ResponseEntity.ok(
            new MessageResponse("Logged out successfully")
        );
    }

}
