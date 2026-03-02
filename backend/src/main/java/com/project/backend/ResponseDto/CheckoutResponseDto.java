package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.project.backend.entity.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDto {
    
    // Pricing
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingCharges;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    
    // Items
    private List<CheckoutItemDto> items;
    private Integer totalItems;
    
    // Delivery
    private AddressDto deliveryAddress;
    private Integer deliveryDays;
    private LocalDate expectedDelivery;
    private Boolean isDeliveryAvailable;
    
    // Payment
    private PaymentMethod paymentMethod;
    private Boolean requiresPayment;
    private String paymentMessage;
    private Boolean isCodAvailable;
    
    // Cart Info
    private Long cartId;
    
    // Validation
    private Boolean isValidForCheckout;
    private List<String> validationErrors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
      
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItemDto {
        private Long productId;
        private String productName;
        private String productImage;
        private Long variantId;
        private String size;
        private String color;
        private String sku;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal mrp;
        private Integer discountPercentage;
        private BigDecimal subtotal;
        private Boolean inStock;
        private Integer availableStock;
    }
    }