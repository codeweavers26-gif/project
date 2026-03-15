package com.project.backend.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentStatsDto {
    private Long totalTransactions;
    private Long successfulPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long refundedPayments;
    
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal weeklyRevenue;
    private BigDecimal monthlyRevenue;
    
    private BigDecimal totalRefundedAmount;
    private Double averageTransactionValue;
    
    private Map<String, Long> paymentsByMethod; // UPI, CARD, NETBANKING
    private Map<String, Long> paymentsByStatus;
    private Map<String, BigDecimal> revenueByMethod;
    
    private List<DailyPaymentSummary> dailyTrend;
}