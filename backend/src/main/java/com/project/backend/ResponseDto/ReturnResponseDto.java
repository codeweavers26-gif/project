package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@Builder
@AllArgsConstructor

@NoArgsConstructor
public class ReturnResponseDto {

    private Long returnId;
    private Long orderId;
    private String status;
    private String reason;
    private String trackingId;
    private BigDecimal refundAmount;
    private List<ReturnItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}