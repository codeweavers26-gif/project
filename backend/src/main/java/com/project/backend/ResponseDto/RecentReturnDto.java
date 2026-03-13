package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.backend.entity.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentReturnDto {
    private Long returnId;
    private String returnNumber;
    private String productName;
    private ReturnStatus status;
    private LocalDateTime requestedDate;
    private BigDecimal refundAmount;
}