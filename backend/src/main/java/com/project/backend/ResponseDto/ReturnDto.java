package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.RefundStatus;
import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDto {
    private Long id;
    private String returnNumber;
    private String status;
    private String reason;
    private String reasonDescription;
    private Integer quantity;
    private BigDecimal refundAmount;
    private BigDecimal restockingFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalRefundAmount;  
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private String productName;
    private BigDecimal itemPrice;
    
    
    private RefundStatus refundStatus;
    private List<ReturnItemDto> items;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;
}