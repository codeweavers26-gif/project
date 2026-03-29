package com.project.backend.requestDto;

import java.util.List;

import com.project.backend.entity.ReturnStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReturnStatusRequest {
    @NotNull
    private String status;

    private String notes;
    private Boolean notifyCustomer;
}