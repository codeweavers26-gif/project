package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnDashboardSummaryDto {

    private Long totalReturns;
    private Long requested;
    private Long approved;
    private Long rejected;
    private Long refunded;

    private Double totalRefundAmount;
}
