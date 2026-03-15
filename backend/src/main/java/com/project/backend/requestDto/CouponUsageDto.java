package com.project.backend.requestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageDto {
    private Long id;
    private String couponCode;
    private String userEmail;
    private String orderNumber;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt;
    private Boolean wasSuccessful;
}