package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CheckoutResponseDto {
    private Long orderId;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private Double subtotal;
    private Double taxAmount;
    private Double shippingCharges;
    private Double discountAmount;
    private Double totalAmount;
    private Instant createdAt;
    
    // New fields for payment flow
    private Boolean requiresPayment;
    private String paymentMessage;
    
    private DeliveryAddressDto deliveryAddress;
    private List<OrderItemDto> items;
}