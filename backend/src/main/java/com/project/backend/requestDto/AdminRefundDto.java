package com.project.backend.requestDto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundDto {
    private Long id;
    private Long transactionId;
    private String razorpayRefundId;
    private BigDecimal amount;
    private String status; // PENDING, PROCESSED, FAILED
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private Long processedBy;
    private String processedByName;
    private String failureReason;
}