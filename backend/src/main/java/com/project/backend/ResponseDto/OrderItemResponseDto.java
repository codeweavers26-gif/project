package com.project.backend.ResponseDto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponseDto {

    private Long productId;
    private Long variantId;
    private String productName;
    private String imageUrl;
    private Double price;
    private Integer quantity;
    private BigDecimal totalPrice;
}
