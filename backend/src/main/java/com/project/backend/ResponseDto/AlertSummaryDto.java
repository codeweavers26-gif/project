package com.project.backend.ResponseDto;

import lombok.Data;

@Data
public class AlertSummaryDto {
    private Long lowStockProducts;
    private Long failedPayments;
    private Long returnRequests;
}

