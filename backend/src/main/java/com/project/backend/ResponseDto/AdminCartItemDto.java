package com.project.backend.ResponseDto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCartItemDto {
    private Long cartId;
    private Long productId;
    private String productName;
    private String productImage;
    private Double productPrice;
    private Integer quantity;
    private Double subtotal;
    private Boolean inStock;
    private Instant addedAt;
}