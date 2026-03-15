package com.project.backend.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.project.backend.entity.CouponStatus;
import com.project.backend.entity.CouponType;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponStatsDto {
    private Long totalCoupons;
    private Long activeCoupons;
    private Long expiredCoupons;
    private Long disabledCoupons;
    private Long totalUses;
    private BigDecimal totalDiscountGiven;
    private Map<String, Long> usageByType;
    private List<CouponUsageDto> recentUsages;
    private Map<String, BigDecimal> discountByMonth;
}
