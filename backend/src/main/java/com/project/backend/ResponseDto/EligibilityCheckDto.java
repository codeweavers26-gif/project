package com.project.backend.ResponseDto;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EligibilityCheckDto {
    private Boolean isEligible;
    private Long orderItemId;
    private String productName;
    private LocalDateTime returnDeadline;
    private String message;
}