package com.project.backend.exception;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final String details;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.details = null;
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BusinessException(String message, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public String getErrorCode() { return errorCode; }
    public String getDetails() { return details; }
}

