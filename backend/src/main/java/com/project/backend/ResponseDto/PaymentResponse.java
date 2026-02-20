package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String paymentId;
    private String orderStatus;
    private String paymentStatus; // Will be SUCCESS, PENDING, or FAILED (matches your enum)
    private boolean cartCleared;
}