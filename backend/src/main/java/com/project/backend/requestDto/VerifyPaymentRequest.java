package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyPaymentRequest {
    
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;
    
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;
    
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
}