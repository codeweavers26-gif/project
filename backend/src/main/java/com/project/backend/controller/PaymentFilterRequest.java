package com.project.backend.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterRequest {
    private String search;
    private String status;
    private String paymentMethod;
    private Long userId;
    private Long orderId;
    private LocalDateTime fromDate;
    private LocalDateTime toDateTime;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean isRefunded;
}