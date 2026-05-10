package com.project.backend.ResponseDto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusHistoryDto {
    private String status;
    private LocalDateTime changedAt;
}
