package com.project.backend.ResponseDto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.Data;

@Data
public class AdminUserReturnResponseDto {
    private Long returnId;
    private String returnNumber;
    private ReturnStatus status;
    private ReturnReason reason;
    private Integer quantity;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    
    private Long orderId;
    private String orderNumber;
    
    private String productName;
    private String productSku;
    private Double productPrice;
    
    private String userEmail;
    private String userName;
}