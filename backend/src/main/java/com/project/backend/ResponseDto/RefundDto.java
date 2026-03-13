package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.backend.entity.RefundMethod;
import com.project.backend.entity.RefundStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {
    private Long id;
    private BigDecimal amount;
    private RefundStatus status;
    private RefundMethod refundMethod;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime updatedAt;
    
    private Long paymentId;
    private String paymentTransactionId;
    private Long returnId;
    private String returnNumber;
    private Long processedBy;
    private String processorName;
    
    private String failureReason;
    private String gatewayResponse;
}

