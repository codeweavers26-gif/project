package com.project.backend.entity;
public enum RefundStatus {
    PENDING,           // Refund initiated, waiting for processing
    PROCESSING,        // Being processed by payment gateway
    COMPLETED,         // Successfully refunded
    FAILED,           // Refund failed
    CANCELLED         // Refund cancelled by admin
}