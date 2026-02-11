package com.project.backend.requestDto;

import lombok.Data;

@Data
public class UpdateReturnStatusDto {
    private String status; 
    private Double refundAmount;
    private String adminComment;
}
