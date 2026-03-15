package com.project.backend.exception;

import java.math.BigDecimal;

public class CouponMinOrderAmountException extends CouponException {
    private final BigDecimal requiredAmount;
    private final BigDecimal currentAmount;
    
    public CouponMinOrderAmountException(String couponCode, BigDecimal requiredAmount, BigDecimal currentAmount) {
        super(String.format("Minimum order amount of ₹%.2f required. Current amount: ₹%.2f", 
              requiredAmount, currentAmount), "COUPON_MIN_ORDER", couponCode);
        this.requiredAmount = requiredAmount;
        this.currentAmount = currentAmount;
    }
}