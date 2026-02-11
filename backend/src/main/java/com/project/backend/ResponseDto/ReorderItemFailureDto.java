package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReorderItemFailureDto {
    private Long productId;
    private String productName;
    private Integer requestedQty;
    private Integer availableQty;
    private String reason;
}
