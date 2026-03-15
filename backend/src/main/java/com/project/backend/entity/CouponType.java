package com.project.backend.entity;

public enum CouponType {
    PERCENTAGE("Percentage discount"),
    FIXED("Fixed amount discount"),
    FREE_SHIPPING("Free shipping");

    private final String description;

    CouponType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}