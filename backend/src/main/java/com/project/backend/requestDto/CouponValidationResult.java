package com.project.backend.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.project.backend.entity.CouponStatus;
import com.project.backend.entity.CouponType;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResult {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private CouponDto coupon;
    private List<String> warnings;
}

