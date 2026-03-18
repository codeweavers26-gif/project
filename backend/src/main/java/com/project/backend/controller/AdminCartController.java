package com.project.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AdminCartItemDto;
import com.project.backend.ResponseDto.AdminCartSummaryDto;
import com.project.backend.ResponseDto.CartStatisticsDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.service.AdminCartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/carts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Cart Management", description = "Production-ready admin cart APIs")
@Slf4j
public class AdminCartController {

    private final AdminCartService adminCartService;

    @Operation(summary = "Get cart items with advanced filters", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/items")
    public ResponseEntity<PageResponseDto<AdminCartItemDto>> getCartItems(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return ResponseEntity.ok(adminCartService.getCartItems(
                userId, productId, userEmail, userName, productName,
                minQuantity, maxQuantity, fromDate, toDate, page, size));
    }

    @Operation(summary = "Get cart summaries (grouped by user)", 
            security = @SecurityRequirement(name = "Bearer Authentication"))
 @GetMapping("/summaries")
 public ResponseEntity<PageResponseDto<AdminCartSummaryDto>> getCartSummaries(
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {
     
     return ResponseEntity.ok(adminCartService.getCartSummaries(page, size));}
    @Operation(summary = "Get cart of user", 
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminCartSummaryDto> getUserCart(@PathVariable Long userId) {
        return ResponseEntity.ok(adminCartService.getUserCartDetails(userId)); 
    }

    @Operation(summary = "Get abandoned carts (>48h old)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/abandoned")
    public ResponseEntity<List<AdminCartSummaryDto>> getAbandonedCarts() {
        return ResponseEntity.ok(adminCartService.getAbandonedCarts());
    }

    @Operation(summary = "Get cart statistics for dashboard", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/statistics")
    public ResponseEntity<CartStatisticsDto> getCartStatistics() {
        return ResponseEntity.ok(adminCartService.getCartStatistics());
    }

}