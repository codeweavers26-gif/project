package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryDto {
    private Double todayRevenue;
    private Double thisMonthRevenue;
    private Double taxCollectedMonth;
    private Long codOrders;
    private Long prepaidOrders;
}
