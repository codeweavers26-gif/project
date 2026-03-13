package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.Map;

import com.project.backend.entity.RefundMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRefundSummaryDto {
    private Long totalRefunds;
    private BigDecimal totalRefundAmount;
    private Long successfulRefunds;
    private Long pendingRefunds;
    private Map<RefundMethod, BigDecimal> refundsByMethod;
}


