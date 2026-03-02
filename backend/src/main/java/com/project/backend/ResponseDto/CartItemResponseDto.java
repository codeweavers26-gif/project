package com.project.backend.ResponseDto;

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
	    private Double price;
	    private Double mrp;
	    private Integer quantity;
	    private Double totalPrice; 
	    private Integer discountPercentage;
}
