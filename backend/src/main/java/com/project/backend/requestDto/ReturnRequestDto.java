package com.project.backend.requestDto;

import java.util.List;

import com.project.backend.entity.ReturnReason;

import lombok.Data;

@Data
public class ReturnRequestDto {
  
    private String reason;    
    private String comment;   

      private List<ReturnItemRequestDto> items;
}
