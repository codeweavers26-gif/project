package com.project.backend.ResponseDto;

import java.math.BigDecimal;

import com.project.backend.entity.ReturnReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnReasonStatsDto {
    private ReturnReason reason;
    private String description;
    private Long count;
    private Double percentage;
    private BigDecimal totalRefundAmount;
}

//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class UserReturnSummaryDto {
//    private Long totalReturns;
//    private Long activeReturns;
//    private Long completedReturns;
//    private Long rejectedReturns;
//    private BigDecimal totalRefundReceived;
//    private List<RecentReturnDto> recentReturns;
//}