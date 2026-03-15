package com.project.backend.controller;

import com.project.backend.entity.User;
import com.project.backend.service.AdminPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Payment Management")
@Slf4j
public class AdminPaymentController {

    private final AdminPaymentService paymentService;

    // @Operation(summary = "Get all payment transactions", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/transactions")
    // public ResponseEntity<PageResponseDto<AdminPaymentTransactionDto>> getAllTransactions(
    //         @RequestParam(required = false) String search,
    //         @RequestParam(required = false) String status,
    //         @RequestParam(required = false) String paymentMethod,
    //         @RequestParam(required = false) Long userId,
    //         @RequestParam(required = false) Long orderId,
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
    //         @RequestParam(required = false) BigDecimal minAmount,
    //         @RequestParam(required = false) BigDecimal maxAmount,
    //         @PageableDefault(size = 20) Pageable pageable) {
        
    //     PaymentFilterRequest filter = PaymentFilterRequest.builder()
    //             .search(search)
    //             .status(status)
    //             .paymentMethod(paymentMethod)
    //             .userId(userId)
    //             .orderId(orderId)
    //             .fromDate(fromDate)
    //             .toDateTime(toDate)
    //             .minAmount(minAmount)
    //             .maxAmount(maxAmount)
    //             .build();
        
    //     return ResponseEntity.ok(paymentService.getAllTransactions(filter, pageable));
    // }

    // @Operation(summary = "Get transaction by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/transactions/{transactionId}")
    // public ResponseEntity<AdminPaymentTransactionDto> getTransactionById(@PathVariable Long transactionId) {
    //     return ResponseEntity.ok(paymentService.getTransactionById(transactionId));
    // }

    // @Operation(summary = "Get transactions by order ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/orders/{orderId}/transactions")
    // public ResponseEntity<List<AdminPaymentTransactionDto>> getTransactionsByOrder(@PathVariable Long orderId) {
    //     return ResponseEntity.ok(paymentService.getTransactionsByOrder(orderId));
    // }

    // @Operation(summary = "Get transactions by user ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/users/{userId}/transactions")
    // public ResponseEntity<PageResponseDto<AdminPaymentTransactionDto>> getTransactionsByUser(
    //         @PathVariable Long userId,
    //         @PageableDefault(size = 20) Pageable pageable) {
    //     return ResponseEntity.ok(paymentService.getTransactionsByUser(userId, pageable));
    // }

    // @Operation(summary = "Get payment dashboard statistics", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/dashboard/stats")
    // public ResponseEntity<AdminPaymentStatsDto> getPaymentStats() {
    //     return ResponseEntity.ok(paymentService.getPaymentStats());
    // }

    // @Operation(summary = "Get payment stats with date range", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/stats")
    // public ResponseEntity<AdminPaymentStatsDto> getPaymentStatsByDateRange(
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
    //     return ResponseEntity.ok(paymentService.getPaymentStatsByDateRange(fromDate, toDate));
    // }

    // @Operation(summary = "Get all refunds", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/refunds")
    // public ResponseEntity<PageResponseDto<AdminRefundDto>> getAllRefunds(
    //         @RequestParam(required = false) String status,
    //         @RequestParam(required = false) Long transactionId,
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
    //         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
    //         @PageableDefault(size = 20) Pageable pageable) {
    //     return ResponseEntity.ok(paymentService.getAllRefunds(status, transactionId, fromDate, toDate, pageable));
    // }

    // @Operation(summary = "Get refund by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/refunds/{refundId}")
    // public ResponseEntity<AdminRefundDto> getRefundById(@PathVariable Long refundId) {
    //     return ResponseEntity.ok(paymentService.getRefundById(refundId));
    // }

    // @Operation(summary = "Process manual refund", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @PostMapping("/transactions/{transactionId}/refund")
    // public ResponseEntity<AdminRefundDto> processRefund(
    //         @PathVariable Long transactionId,
    //         @Valid @RequestBody ProcessRefundRequest request,
    //         @AuthenticationPrincipal User admin) {
    //     return ResponseEntity.ok(paymentService.processRefund(transactionId, request, admin));
    // }

    // @Operation(summary = "Retry failed payment", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @PostMapping("/transactions/{transactionId}/retry")
    // public ResponseEntity<Map<String, String>> retryPayment(
    //         @PathVariable Long transactionId,
    //         @AuthenticationPrincipal User admin) {
    //     paymentService.retryPayment(transactionId, admin);
    //     return ResponseEntity.ok(Map.of("message", "Payment retry initiated"));
    // }

    // @Operation(summary = "Mark payment as failed", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @PostMapping("/transactions/{transactionId}/mark-failed")
    // public ResponseEntity<AdminPaymentTransactionDto> markPaymentAsFailed(
    //         @PathVariable Long transactionId,
    //         @RequestBody Map<String, String> request,
    //         @AuthenticationPrincipal User admin) {
    //     String reason = request.getOrDefault("reason", "Marked failed by admin");
    //     return ResponseEntity.ok(paymentService.markPaymentAsFailed(transactionId, reason, admin));
    // }

    // @Operation(summary = "Export payment report", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/export")
    // public ResponseEntity<byte[]> exportPaymentReport(
    //         @RequestParam String format, // PDF, EXCEL, CSV
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
    //     byte[] report = paymentService.exportPaymentReport(format, fromDate, toDate);
        
    //     String filename = String.format("payment_report_%s_to_%s.%s", 
    //             fromDate.toLocalDate(), toDate.toLocalDate(), format.toLowerCase());
        
    //     return ResponseEntity.ok()
    //             .header("Content-Disposition", "attachment; filename=" + filename)
    //             .body(report);
    // }

    // @Operation(summary = "Get payment methods breakdown", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/analytics/methods")
    // public ResponseEntity<List<PaymentMethodAnalytics>> getPaymentMethodAnalytics(
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
    //     return ResponseEntity.ok(paymentService.getPaymentMethodAnalytics(fromDate, toDate));
    // }

    // @Operation(summary = "Get hourly payment trend", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @GetMapping("/analytics/hourly")
    // public ResponseEntity<List<HourlyPaymentTrend>> getHourlyPaymentTrend(
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
    //     return ResponseEntity.ok(paymentService.getHourlyPaymentTrend(date));
    // }

    // @Operation(summary = "Verify payment manually", security = @SecurityRequirement(name = "Bearer Authentication"))
    // @PostMapping("/transactions/{transactionId}/verify")
    // public ResponseEntity<AdminPaymentTransactionDto> verifyPaymentManually(
    //         @PathVariable Long transactionId,
    //         @AuthenticationPrincipal User admin) {
    //     return ResponseEntity.ok(paymentService.verifyPaymentManually(transactionId, admin));
    // }
}

// Supporting DTOs
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// class ProcessRefundRequest {
//     private BigDecimal amount;
//     private String reason;
//     private Boolean notifyCustomer;
//     private String refundMethod; // ORIGINAL, STORE_CREDIT, BANK_TRANSFER
// }

// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// class PaymentMethodAnalytics {
//     private String method;
//     private Long transactionCount;
//     private BigDecimal totalAmount;
//     private Double successRate;
//     private Double averageAmount;
// }

// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// class HourlyPaymentTrend {
//     private Integer hour;
//     private Long count;
//     private BigDecimal amount;
//     private Long successCount;
//     private Long failureCount;
// }