package com.project.backend.requestDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveReturnRequest {
    private String adminNotes;
    private BigDecimal restockingFee;
    private Boolean notifyCustomer = true;
}

