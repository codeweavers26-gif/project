package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor  
public class LowStockItemDto {
    private String productName;
    private Integer currentStock;
}