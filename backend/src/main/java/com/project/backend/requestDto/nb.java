package com.project.backend.requestDto;
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
class PaymentMethodAnalytics {
    private String method;
    private Long transactionCount;
    private BigDecimal totalAmount;
    private Double successRate;
    private Double averageAmount;
}
