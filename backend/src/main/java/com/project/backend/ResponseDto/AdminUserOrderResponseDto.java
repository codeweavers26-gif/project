package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.PaymentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserOrderResponseDto {

    private Long orderId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private Double subtotal;
    private Double taxAmount;
    private Double shippingCharges;
    private Double discountAmount;
    private Double totalAmount;

    private OrderAddressDto deliveryAddress;
    private List<OrderItemAdminDto> items;

    private Instant createdAt;
}
