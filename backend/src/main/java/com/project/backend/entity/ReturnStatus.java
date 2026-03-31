package com.project.backend.entity;

public enum ReturnStatus {

    REQUESTED,         
    APPROVED,
    REJECTED,
    PICKUP_SCHEDULED,   
    PICKED_UP,          
    IN_TRANSIT,        
    RECEIVED,         

    QC_PENDING,
    QC_PASSED,
    QC_FAILED,
    REFUND_PENDING,
    REFUNDED,

    CANCELLED,
    FAILED
}