package com.project.backend.entity;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PICKED_UP,
    PENDING_APPROVAL,
    PENDING_PICKUP,
    PICKUP_SCHEDULED,
    PICKUP_COMPLETED,
    QC_PENDING,           
    QC_IN_PROGRESS,   
    REFUND_PENDING,          
    REFUND_COMPLETED,        
    REPLACEMENT_PENDING,    
    REPLACEMENT_SHIPPED,    
    REPLACEMENT_DELIVERED,  
    QC_PASSED,              
    QC_FAILED,  
    CANCELLED      ,
    COMPLETED
}
