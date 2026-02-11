package com.project.backend.requestDto;

import com.project.backend.entity.ReturnReason;

import lombok.Data;

@Data
public class ReturnRequestDto {
    private Long orderItemId;
    private Integer quantity;
    private ReturnReason reason;    
    private String comment;   
}
