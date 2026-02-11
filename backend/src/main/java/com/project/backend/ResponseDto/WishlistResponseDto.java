package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistResponseDto {
    private Long productId;
    private String productName;
    private String image;
    private Double price;
    private Boolean inStock;
}
