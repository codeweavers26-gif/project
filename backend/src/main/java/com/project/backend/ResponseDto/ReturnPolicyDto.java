package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.List;

import com.project.backend.entity.ReturnReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPolicyDto {
    private Integer returnWindowDays;
    private Boolean isFreeReturns;
    private BigDecimal restockingFeePercentage;
    private List<String> eligibleConditions;
    private List<String> nonEligibleItems;
    private String refundTimeline;
    private List<ReturnReason> acceptableReasons;
}