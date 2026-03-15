package com.project.backend.exception;


import com.project.backend.exception.BusinessException;
import lombok.Getter;

@Getter
public class CouponException extends BusinessException {
    private final String couponCode;
    
    public CouponException(String message, String couponCode) {
        super(message, "COUPON_ERROR");
        this.couponCode = couponCode;
    }
    
    public CouponException(String message, String errorCode, String couponCode) {
        super(message, errorCode);
        this.couponCode = couponCode;
    }
}