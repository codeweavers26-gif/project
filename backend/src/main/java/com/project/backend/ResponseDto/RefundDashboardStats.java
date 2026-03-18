package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
public class RefundDashboardStats {
    private Long totalRefunds;
    private Long pendingRefunds;
    private Long completedRefunds;
    private Long failedRefunds;
    private BigDecimal totalRefundAmount;
    private BigDecimal averageRefundAmount;
    private Map<RefundStatus, Long> refundsByStatus;
    private Map<RefundMethod, BigDecimal> refundsByMethod;

}