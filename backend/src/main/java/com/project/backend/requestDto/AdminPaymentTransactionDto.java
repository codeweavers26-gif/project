package com.project.backend.requestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentTransactionDto {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    
    // Razorpay details
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    
    // Payment details
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status; // CREATED, PENDING, SUCCESS, FAILED, REFUNDED
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    
    // Additional info
    private String failureReason;
    private String gatewayResponse;
    
    // Refund info
    private BigDecimal refundedAmount;
    private LocalDateTime refundedAt;
    private String refundId;
}
