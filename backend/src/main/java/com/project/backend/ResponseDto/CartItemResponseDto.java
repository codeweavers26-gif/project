package com.project.backend.ResponseDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

	 private Long cartItemId;
	    private Long productId;
	    private Long variantId;
	    private String color;
	    private String size;
	    private String productName;
	    private String imageUrl;
	    private BigDecimal price;
	    private BigDecimal mrp;
	    private Integer quantity;
	    private BigDecimal totalPrice; 
	    private Integer discountPercentage;
}
