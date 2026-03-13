package com.project.backend.requestDto;

import java.math.BigDecimal;

import com.project.backend.entity.RefundMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRefundRequest {
    @NotNull(message = "Return ID is required")
    private Long returnId;

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Refund method is required")
    private RefundMethod refundMethod;

    private String notes;
    private Boolean notifyCustomer = true;
}
