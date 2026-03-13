package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailRefundRequest {
    @NotBlank(message = "Failure reason is required")
    private String failureReason;

    private String gatewayResponse;
    private String notes;
}

