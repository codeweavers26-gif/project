package com.project.backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {

    // ── Active lifecycle statuses (used in new orders) ──────────────────
    PLACED,          // Order confirmed, awaiting processing
    PROCESSING,      // Warehouse is packing / preparing shipment
    SHIPPED,         // Handed to courier, tracking available
    DELIVERED,       // Successfully delivered to customer

    // ── Terminal / side-branch statuses ─────────────────────────────────
    CANCELLED,       // Order cancelled (before shipping)
    RETURN_REQUESTED,// Customer initiated return after delivery

    // ── Legacy statuses — kept for backward-compatibility only ───────────
    // These existed in old orders; do NOT use for new order creation.
    PAID,            // Old "payment confirmed" order state (now tracked via paymentStatus)
    PENDING,         // Old COD "awaiting payment" state
    PENDING_PAYMENT,
    PREPAID,
    CREATED,
    PARTIALLY_CANCELLED;

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
