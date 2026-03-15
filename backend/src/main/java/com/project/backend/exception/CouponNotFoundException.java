package com.project.backend.exception;


public class CouponNotFoundException extends CouponException {
    public CouponNotFoundException(String couponCode) {
        super("Coupon not found with code: " + couponCode, "COUPON_NOT_FOUND", couponCode);
    }
    
    public CouponNotFoundException(Long id) {
        super("Coupon not found with ID: " + id, "COUPON_NOT_FOUND", null);
    }
}