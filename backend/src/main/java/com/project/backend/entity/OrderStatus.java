package com.project.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    PLACED,
    RETURN_REQUESTED,
    PENDING,
    PREPAID,
    PENDING_PAYMENT;
    
    

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null) return null;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
