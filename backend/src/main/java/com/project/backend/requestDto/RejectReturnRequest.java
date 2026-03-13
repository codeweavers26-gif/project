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
public class RejectReturnRequest {
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    private String adminNotes;
    private Boolean notifyCustomer = true;
}