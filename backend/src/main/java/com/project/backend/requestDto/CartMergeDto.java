package com.project.backend.requestDto;

import lombok.Data;

@Data
public class CartMergeDto {
    private Long productId;
    private Integer quantity;
}

