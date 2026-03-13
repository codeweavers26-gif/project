package com.project.backend.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnEligibilityRequest {
    @NotNull
    private Long orderItemId;

    @NotNull
    private Long productId;
}