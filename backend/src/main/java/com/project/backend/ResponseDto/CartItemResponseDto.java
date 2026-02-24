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

	 private Long cartId;
	    private Long productId;
	    private String productName;
	    private String imageUrl;
	    private Double price;
	    private Integer quantity;
	    private Double totalPrice; 
}
