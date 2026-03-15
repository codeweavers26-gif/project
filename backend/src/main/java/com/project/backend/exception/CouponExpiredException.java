package com.project.backend.exception;


public class CouponExpiredException extends CouponException {
    public CouponExpiredException(String couponCode) {
        super("Coupon has expired: " + couponCode, "COUPON_EXPIRED", couponCode);
    }
}