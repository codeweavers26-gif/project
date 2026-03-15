package com.project.backend.exception;

public class RefundProcessingException extends BusinessException {
    public RefundProcessingException(String message) {
        super(message, "REFUND_PROCESSING_ERROR");
    }
}
