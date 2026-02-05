package com.project.backend.ResponseDto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDto {
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private Double total;
}
