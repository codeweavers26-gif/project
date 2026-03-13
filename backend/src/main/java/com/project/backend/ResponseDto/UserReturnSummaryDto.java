package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReturnSummaryDto {
    private Long totalReturns;
    private Long activeReturns;
    private Long completedReturns;
    private Long rejectedReturns;
    private BigDecimal totalRefundReceived;
    private List<RecentReturnDto> recentReturns;
}
