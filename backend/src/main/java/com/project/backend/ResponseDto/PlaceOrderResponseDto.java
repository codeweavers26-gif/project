package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class PlaceOrderResponseDto {

    private Long orderId;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingCharges;
    private BigDecimal totalAmount;

    private String orderStatus;     
    private String paymentStatus; 
    private String paymentMethod;  

    private LocalDate expectedDelivery;
    private Integer deliveryDays;
    private LocalDateTime paymentExpiry;

    private DeliveryAddressDto deliveryAddress;
    private String message;
    private Boolean requiresPayment;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressDto {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
}