package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import com.project.backend.entity.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDto {

    // Order Info
    private Long orderId;
    private Double totalAmount;
    private Double taxAmount;
    private Double shippingCharges;
    private Double discountAmount;
    private String paymentMethod;
    private String paymentStatus;
    private OrderStatus status;
    private Instant createdAt;
    
    // 👤 USER INFO - Add these fields
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;  // if you have phone in User entity
    
    // 📦 Delivery Address
    private String deliveryAddressLine1;
    private String deliveryAddressLine2;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPostalCode;
    private String deliveryCountry;
    
    // 🛒 Items
    private List<OrderItemResponseDto> items;
}