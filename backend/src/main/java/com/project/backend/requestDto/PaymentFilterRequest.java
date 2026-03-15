package com.project.backend.requestDto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

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
