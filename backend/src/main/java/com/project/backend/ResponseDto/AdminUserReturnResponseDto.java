package com.project.backend.ResponseDto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserReturnResponseDto {

    private Long returnId;

    private Long userId;
    private String userEmail;

    private Long orderId;
    private Long orderItemId;

    private Long productId;
    private String productName;

    private Integer quantity;

    private String reason;      
    private String status;    

    private Double refundAmount;

    private Instant requestedAt;
}
