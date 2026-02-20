	package com.project.backend.mapper;
	
	import java.util.ArrayList;
import java.util.stream.Collectors;

import com.project.backend.ResponseDto.OrderItemResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;
	
	public class OrderMapper {
	
		public static OrderResponseDto toDto(Order order) {

		    User user = order.getUser();  // Get user from order
		    
		    return OrderResponseDto.builder()
		            // Order Info
		            .orderId(order.getId())
		            .totalAmount(order.getTotalAmount())
		            .taxAmount(order.getTaxAmount())
		            .shippingCharges(order.getShippingCharges())
		            .discountAmount(order.getDiscountAmount())
		            .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
		            .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
		            .status(order.getStatus())
		            .createdAt(order.getCreatedAt())
		            
		            // 👤 USER INFO
		            .userId(user != null ? user.getId() : null)
		            .userName(user != null ? user.getName() : null)
		            .userEmail(user != null ? user.getEmail() : null)
		            
		            // 📦 Delivery Address
		            .deliveryAddressLine1(order.getDeliveryAddressLine1())
		            .deliveryAddressLine2(order.getDeliveryAddressLine2())
		            .deliveryCity(order.getDeliveryCity())
		            .deliveryState(order.getDeliveryState())
		            .deliveryPostalCode(order.getDeliveryPostalCode())
		            .deliveryCountry(order.getDeliveryCountry())
		            
		            // 🛒 Items
		            .items(order.getItems() != null ? 
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
		                    : new ArrayList<>())
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
