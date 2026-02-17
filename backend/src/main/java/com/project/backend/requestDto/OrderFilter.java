package com.project.backend.requestDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderFilter {
    private Long userId;
    private String status;       
    private String paymentStatus;   
    private String paymentMethod;   
    private Double minAmount;
    private Double maxAmount;
    private String fromDate;  
    private String toDate;          
    private String search;          
    private String sortBy;           
    private String sortDirection;    
}