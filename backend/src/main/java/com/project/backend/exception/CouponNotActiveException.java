package com.project.backend.exception;

import java.time.LocalDateTime;

public class CouponNotActiveException extends CouponException {
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    
    public CouponNotActiveException(String couponCode, LocalDateTime validFrom, LocalDateTime validTo) {
        super("Coupon is not active. Valid from: " + validFrom + " to: " + validTo, 
              "COUPON_NOT_ACTIVE", couponCode);
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}