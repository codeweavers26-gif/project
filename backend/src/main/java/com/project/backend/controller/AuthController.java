package com.project.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AuthResponse;
import com.project.backend.ResponseDto.MessageResponse;
import com.project.backend.entity.Role;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.AuthRequest;
import com.project.backend.requestDto.RegisterRequest;
import com.project.backend.requestDto.RequestOtpRequest;
import com.project.backend.requestDto.TokenRefreshRequest;
import com.project.backend.requestDto.VerifyOtpRequest;
import com.project.backend.service.AuthService;
import com.project.backend.service.OtpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Handles login, register, refresh token, logout")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @Operation(summary = "Register a new customer account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Validated @RequestBody RegisterRequest req) {

        AuthResponse resp = authService.register(req, Role.CUSTOMER);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Register a new admin account")
    @PostMapping("/admin/register")
    public ResponseEntity<AuthResponse> adminRegister(
            @Validated @RequestBody RegisterRequest req) {

        AuthResponse resp = authService.register(req, Role.ADMIN);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Admin login")
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.login(req, Role.ADMIN));
    }

    @Operation(summary = "Customer login")
    @PostMapping("/customer/login")
    public ResponseEntity<AuthResponse> customerLogin(@RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.login(req, Role.CUSTOMER));
    }

    @Operation(summary = "Refresh JWT access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Validated @RequestBody TokenRefreshRequest request) {

        AuthResponse resp = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Logout user (invalidate refresh tokens)")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.ok(
                new MessageResponse("Logged out successfully")
            );
        }

        authService.logout(authentication.getName());
        return ResponseEntity.ok(
            new MessageResponse("Logged out successfully")
        );
    }
@PostMapping("/verify-otp")
public ResponseEntity<AuthResponse> verifyOtp(
        @RequestBody VerifyOtpRequest req,
        HttpServletRequest request) {

    String ip = request.getRemoteAddr();
    String userAgent = request.getHeader("User-Agent");

    return ResponseEntity.ok(
        authService.verifyOtp(
            req.getIdentifier(),
            req.getOtp(),
            ip,
            userAgent
        )
    );
}

@PostMapping("/reset-password")
public ResponseEntity<MessageResponse> resetPassword(
        @RequestBody java.util.Map<String, String> body) {
    authService.resetPassword(body.get("identifier"), body.get("otp"), body.get("newPassword"));
    return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
}

@PostMapping("/request-otp")
public ResponseEntity<MessageResponse> requestOtp(
        @Validated @RequestBody RequestOtpRequest req,
        HttpServletRequest request) {

    String ip = request.getRemoteAddr();
    String userAgent = request.getHeader("User-Agent");

    otpService.requestOtp(req.getIdentifier(), ip, userAgent);

    return ResponseEntity.ok(
            new MessageResponse("OTP sent successfully")
    );
}
}
