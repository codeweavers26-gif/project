package com.project.backend.ResponseDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemDto {
	  private Long productId;
	    private Long variantId;   

	    private String productName;
	    private String size;
	    private String color;

	    private Double price;
	    private Integer quantity;
	    private Double total;
}
