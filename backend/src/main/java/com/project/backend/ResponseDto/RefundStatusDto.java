package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.RefundStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatusDto {
    private Long refundId;
    private RefundStatus status;
    private BigDecimal amount;
    private String transactionId;
    private LocalDateTime estimatedCompletion;
    private String message;
    private List<StatusHistory> history;
}

