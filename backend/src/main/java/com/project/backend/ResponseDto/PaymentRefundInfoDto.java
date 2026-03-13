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
public class PaymentRefundInfoDto {
    private Long paymentId;
    private String transactionId;
    private BigDecimal paidAmount;
    private BigDecimal refundedAmount;
    private BigDecimal availableForRefund;
    private List<RefundDto> previousRefunds;
    private Boolean canRefund;
    private String message;
}