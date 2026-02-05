package com.project.backend.ResponseDto;


import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

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

    private DeliveryAddressDto deliveryAddress;

    private List<OrderItemDto> items;

    private Instant createdAt;
}
