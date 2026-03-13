package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.Map;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDashboardStats {
    private Long totalReturns;
    private Long pendingApproval;
    private Long approved;
    private Long rejected;
    private Long processing;
    private Long completed;
    private BigDecimal totalRefundAmount;
    private Double averageProcessingTime;
    private Map<ReturnStatus, Long> returnsByStatus;
    private Map<ReturnReason, Long> returnsByReason;
}
