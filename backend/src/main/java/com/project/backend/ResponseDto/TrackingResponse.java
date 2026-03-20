package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackingResponse {
    private String status;
    private String location;
    private String estimatedDeliveryDate;
}