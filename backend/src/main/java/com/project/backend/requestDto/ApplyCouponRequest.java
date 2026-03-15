package com.project.backend.requestDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.project.backend.entity.CouponStatus;
import com.project.backend.entity.CouponType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {
    @NotBlank(message = "Coupon code is required")
    private String couponCode;

    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.01", message = "Order amount must be greater than 0")
    private BigDecimal orderAmount;

    private Long userId;
    private Set<Long> productIds;
    private Set<Long> categoryIds;
    private String paymentMethod;
    private Boolean isFirstOrder;
    private Boolean isNewUser;
}



