package com.project.backend.requestDto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerReturnDto {
    private Long returnId;
    private Long orderId;
    private String productName;
    private Integer quantity;
    private String status;
    private Double refundAmount;
    private Instant requestedAt;
}
