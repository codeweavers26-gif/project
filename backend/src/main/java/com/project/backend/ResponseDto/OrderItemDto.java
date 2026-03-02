package com.project.backend.ResponseDto;


import lombok.Builder;
import lombok.Data;

@Data
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
