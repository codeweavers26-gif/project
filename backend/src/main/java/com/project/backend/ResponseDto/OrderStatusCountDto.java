package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderStatusCountDto {
    private Long pending;
    private Long shipped;
    private Long cancelled;
    private Long delivered;
    private Long paid;
    private Long placed;
    private Long returnRequested;
    
}
