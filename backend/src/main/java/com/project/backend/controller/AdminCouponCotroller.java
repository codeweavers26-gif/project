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
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin  Coupon Managemnet")
public class AdminCouponCotroller {

      private final CouponService couponService; 
      private final UserRepository userRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
  @Operation(summary = "Create coupon", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping
    public ResponseEntity<CouponDto> createCoupon(
            @Valid @RequestBody CouponDto couponDto,
            Authentication authentication) {  // ← Use Authentication, not @AuthenticationPrincipal
        
        User admin = getCurrentUser(authentication);
        return ResponseEntity.ok(couponService.createCoupon(couponDto, admin.getId()));
    }

    @Operation(summary = "Bulk create coupons", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/admin/coupons/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponDto>> bulkCreateCoupons(
            @Valid @RequestBody BulkCouponCreateRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(couponService.bulkCreateCoupons(request, admin.getId()));
    }

    @Operation(summary = "Get all coupons with filters", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDto<CouponDto>> getAllCoupons(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CouponType type,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(couponService.getAllCoupons(search, type, status, fromDate, toDate, pageable));
    }

    @Operation(summary = "Get coupon by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{id}")
    public ResponseEntity<CouponDto> getCouponById(
            @PathVariable Long id,
            Authentication authentication) {
        getCurrentUser(authentication);
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @Operation(summary = "Update coupon", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/{id}")
    public ResponseEntity<CouponDto> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponDto couponDto,
            Authentication authentication) {
        User admin = getCurrentUser(authentication);
        log.info("Admin {} updating coupon ID: {}", admin.getEmail(), id);
        return ResponseEntity.ok(couponService.updateCoupon(id, couponDto, admin.getId()));
    }


    @Operation(summary = "Delete coupon", security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable Long id,
            Authentication authentication) {
        User admin = getCurrentUser(authentication);
        log.info("Admin {} deleting coupon ID: {}", admin.getEmail(), id);
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

     @Operation(summary = "Change coupon status", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/status")
    public ResponseEntity<CouponDto> changeCouponStatus(
            @PathVariable Long id,
            @RequestParam CouponStatus status,
            Authentication authentication) {
        User admin = getCurrentUser(authentication);
        log.info("Admin {} changing coupon {} status to: {}", admin.getEmail(), id, status);
        return ResponseEntity.ok(couponService.changeCouponStatus(id, status, admin.getId()));
    }

   @Operation(summary = "Get coupon statistics", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/stats")
    public ResponseEntity<CouponStatsDto> getCouponStats(Authentication authentication) {
        getCurrentUser(authentication);
        return ResponseEntity.ok(couponService.getCouponStats());
    }

    @Operation(summary = "Get coupon usage history", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{id}/usage")
    public ResponseEntity<PageResponseDto<CouponUsageDto>> getCouponUsage(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        getCurrentUser(authentication);
        return ResponseEntity.ok(couponService.getCouponUsage(id, pageable));
    }

}
