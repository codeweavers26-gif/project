package com.project.backend.controller;

import com.project.backend.entity.CouponStatus;
import com.project.backend.entity.CouponType;
import com.project.backend.entity.CouponUsage;
import com.project.backend.entity.User;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.ApplyCouponRequest;
import com.project.backend.requestDto.BulkCouponCreateRequest;
import com.project.backend.requestDto.CouponDto;
import com.project.backend.requestDto.CouponStatsDto;
import com.project.backend.requestDto.CouponUsageDto;
import com.project.backend.requestDto.CouponValidationResult;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Management")
public class CouponController {

    private final UserRepository userRepository;
    private final CouponService couponService;
  private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

      @Operation(summary = "Validate and apply coupon", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/user/coupons/validate")
    public ResponseEntity<CouponValidationResult> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            User user = getCurrentUser(auth);
            request.setUserId(user.getId());
        }
        return ResponseEntity.ok(couponService.validateAndApplyCoupon(request));
    }

    @Operation(summary = "Apply coupon to order", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/user/orders/{orderId}/coupons/apply")

    public ResponseEntity<CouponUsage> applyCouponToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ApplyCouponRequest request,
            Authentication auth) {
        User user = getCurrentUser(auth);
        request.setUserId(user.getId());
        return ResponseEntity.ok(couponService.applyCouponToOrder(request, orderId));
    }

    @Operation(summary = "Remove coupon from order", security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/user/orders/{orderId}/coupons")

    public ResponseEntity<Void> removeCouponFromOrder(
            @PathVariable Long orderId,
            Authentication auth) {
        couponService.removeCouponFromOrder(orderId);
        return ResponseEntity.noContent().build();
    }


      @Operation(summary = "Get available coupons for user", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/user/coupons/available")

    public ResponseEntity<List<CouponDto>> getAvailableCoupons(
            Authentication auth,
            @RequestParam BigDecimal orderAmount) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(couponService.getAvailableCouponsForUser(user.getId(), orderAmount));
    }

     @Operation(summary = "Get coupon by code", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/public/coupons/{code}")
    public ResponseEntity<CouponDto> getCouponByCode(@PathVariable String code) {
        return ResponseEntity.ok(couponService.getCouponByCode(code));
    }
}