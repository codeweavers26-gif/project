package com.project.backend.ResponseDto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.Data;

@Data
public class AdminUserReturnResponseDto {
    private Long returnId;
    private String returnNumber;
    private String status;
    private String reason;
    private Integer quantity;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    
    private Long orderId;
    private String orderNumber;
    
    private String productName;
    private String productSku;
    private Double productPrice;
    private List<ReturnItemDto> items;
    private String userEmail;
    private String userName;
}