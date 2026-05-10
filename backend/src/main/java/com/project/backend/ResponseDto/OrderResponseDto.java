package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import com.project.backend.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {

    private Long orderId;
    private Double totalAmount;
    private Double subtotal;
    private Double taxAmount;
    private Double shippingCharges;
    private Double discountAmount;
    private String paymentMethod;
    private String paymentStatus;
    private OrderStatus status;
    private Instant createdAt;

    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    private String deliveryAddressLine1;
    private String deliveryAddressLine2;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPostalCode;
    private String deliveryCountry;

    private List<OrderItemResponseDto> items;

    /** Chronological status progression (auto-backfilled on each update) */
    private List<OrderStatusHistoryDto> statusHistory;
}