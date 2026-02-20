package com.project.backend.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "Get cart summaries (grouped by user)", security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/summaries")
    public ResponseEntity<List<AdminCartSummaryDto>> getCartSummaries() {
        return ResponseEntity.ok(adminCartService.getCartSummaries());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminCartSummaryDto> getUserCart(@PathVariable Long userId) {
        return ResponseEntity.ok(adminCartService.getUserCart(userId)); // Make sure method name matches
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

//    @Operation(summary = "Remove specific cart item", security = @SecurityRequirement(name = "Bearer Authentication"))
//    @DeleteMapping("/items/{cartId}")
//    public ResponseEntity<Void> removeCartItem(@PathVariable Long cartId) {
//        adminCartService.removeCartItem(cartId);
//        return ResponseEntity.ok().build();
//    }

    @Operation(summary = "Clear entire user cart", security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/users/{userId}/clear")
    public ResponseEntity<Void> clearUserCart(@PathVariable Long userId) {
        adminCartService.clearUserCart(userId);
        return ResponseEntity.ok().build();
    }

//    @Operation(summary = "Update cart item quantity", security = @SecurityRequirement(name = "Bearer Authentication"))
//    @PatchMapping("/items/{cartId}")
//    public ResponseEntity<AdminCartItemDto> updateCartItemQuantity(
//            @PathVariable Long cartId,
//            @RequestParam Integer quantity) {
//        return ResponseEntity.ok(adminCartService.updateCartItemQuantity(cartId, quantity));
//    }

//    @Operation(summary = "Export cart data to CSV")
//    @GetMapping("/export")
//    public ResponseEntity<byte[]> exportCartData() {
//        List<List<String>> csvData = adminCartService.exportCartData();
//        
//        // Convert to CSV
//        StringBuilder csv = new StringBuilder();
//        for (List<String> row : csvData) {
//            csv.append(String.join(",", row)).append("\n");
//        }
//        
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.TEXT_PLAIN);
//        headers.setContentDisposition(ContentDisposition.builder("attachment")
//                .filename("cart-export-" + LocalDate.now() + ".csv").build());
//        
//        return new ResponseEntity<>(csv.toString().getBytes(), headers, HttpStatus.OK);
//    }

    @Operation(summary = "Bulk delete abandoned carts", security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/abandoned/clear")
    public ResponseEntity<Map<String, Object>> clearAbandonedCarts() {
        List<AdminCartSummaryDto> abandoned = adminCartService.getAbandonedCarts();
        
        for (AdminCartSummaryDto cart : abandoned) {
            adminCartService.clearUserCart(cart.getUserId());
        }
        
        return ResponseEntity.ok(Map.of(
                "message", "Abandoned carts cleared",
                "count", abandoned.size()
        ));
    }
}