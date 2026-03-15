package com.project.backend.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCouponCreateRequest {
    @Min(value = 1, message = "Number of coupons must be at least 1")
    @Max(value = 1000, message = "Cannot create more than 1000 coupons at once")
    private Integer count;

    @NotBlank(message = "Prefix is required")
    @Size(min = 2, max = 10, message = "Prefix must be between 2 and 10 characters")
    private String prefix;

    private CouponDto template;
}