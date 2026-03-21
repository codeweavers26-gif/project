package com.project.backend.requestDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.project.backend.entity.PaymentMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyNowCheckoutResponseDto {
    
    private Long productId;
    private String productName;
    private String productImage;
    private Long variantId;
    private String size;
    private String color;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal mrp;
    private Integer discountPercentage;
    private BigDecimal subtotal;
    
    private AddressDto deliveryAddress;
    private Integer deliveryDays;
    private LocalDate expectedDelivery;
    
    private BigDecimal taxAmount;
    private BigDecimal shippingCharges;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    
    private PaymentMethod paymentMethod;
    private Boolean requiresPayment;
    private Boolean isCodAvailable;
    
    private List<String> validationErrors;
    private Boolean isValid;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private Long addressId;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
}