package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReorderItemSuccessDto {
    private Long productId;
    private String productName;
    private Integer quantityAdded;
}
