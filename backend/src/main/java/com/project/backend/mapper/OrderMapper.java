	package com.project.backend.mapper;
	
	import java.util.stream.Collectors;

import com.project.backend.ResponseDto.OrderItemResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
	
	public class OrderMapper {
	
		public static OrderResponseDto toDto(Order order) {

		    return OrderResponseDto.builder()
		            .orderId(order.getId())
		            .totalAmount(order.getTotalAmount())
		            .status(order.getStatus())
		            .createdAt(order.getCreatedAt())
		            .items(
		                    order.getItems().stream()
		                            .map(item -> OrderItemResponseDto.builder()
		                                    .productId(item.getProduct().getId())
		                                    .productName(item.getProduct().getName())
		                                    .imageUrl(getPrimaryImage(item.getProduct()))
		                                    .price(item.getPrice())
		                                    .quantity(item.getQuantity())
		                                    .totalPrice(item.getPrice() * item.getQuantity())
		                                    .build())
		                            .collect(Collectors.toList())
		            )
		            .build();
		}

	    
	    private static String getPrimaryImage(Product product) {
	        return product.getImages() != null && !product.getImages().isEmpty()
	                ? product.getImages().stream()
	                    .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
	                    .findFirst()
	                    .map(ProductImage::getImageUrl)
	                    .orElse(null)
	                : null;
	    }

	}
