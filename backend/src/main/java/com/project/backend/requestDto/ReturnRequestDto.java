package com.project.backend.requestDto;

import lombok.Data;

@Data
public class ReturnRequestDto {
    private Long orderItemId;
    private Integer quantity;
    private String reason;    
    private String comment;   
}
