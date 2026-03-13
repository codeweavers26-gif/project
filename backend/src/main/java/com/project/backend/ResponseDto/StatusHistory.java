package com.project.backend.ResponseDto;

import java.time.LocalDateTime;

import com.project.backend.entity.RefundStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusHistory {
    private RefundStatus status;
    private LocalDateTime timestamp;
    private String note;
}