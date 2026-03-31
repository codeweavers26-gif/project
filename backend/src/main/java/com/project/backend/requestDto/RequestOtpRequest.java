package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestOtpRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier; // email OR phone
}