package com.project.backend.entity;

public enum CouponStatus {
    ACTIVE("Coupon is active and can be used"),
    EXPIRED("Coupon has expired"),
    DISABLED("Coupon is disabled by admin"),
    SCHEDULED("Coupon is scheduled for future");

    private final String description;

    CouponStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}