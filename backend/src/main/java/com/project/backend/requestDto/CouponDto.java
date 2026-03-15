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
public class CouponDto {
    private Long id;

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code can only contain uppercase letters, numbers, underscore and hyphen")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @NotNull(message = "Coupon type is required")
    private CouponType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @DecimalMax(value = "100", message = "Percentage discount cannot exceed 100%")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0", message = "Maximum discount amount cannot be negative")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Valid from date is required")
    @FutureOrPresent(message = "Valid from date cannot be in the past")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid to date is required")
    @Future(message = "Valid to date must be in the future")
    private LocalDateTime validTo;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Usage per user must be at least 1")
    private Integer usagePerUser;

    private CouponStatus status;
    private Integer totalUsedCount;
    private Set<Long> applicableCategoryIds;
    private Set<Long> applicableProductIds;
    private Set<Long> excludedCategoryIds;
    private Set<Long> excludedProductIds;
    private Boolean isFirstOrderOnly;
    private Boolean isNewUserOnly;
    private String applicablePaymentMethods;
    private String applicableUserTiers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

