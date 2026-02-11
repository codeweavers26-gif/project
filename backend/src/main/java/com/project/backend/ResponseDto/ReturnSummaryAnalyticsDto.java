package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnSummaryAnalyticsDto {

    private Long totalReturns;
    private Long pendingReturns;
    private Long approvedReturns;
    private Long rejectedReturns;
    private Long completedReturns;

    private Double totalRefundAmount;
}
