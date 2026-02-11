package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopReturnedProductDto {

    private Long productId;
    private String productName;
    private Long returnCount;
}
