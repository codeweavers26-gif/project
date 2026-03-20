package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentResponse {
    private String shipmentId;
    private String trackingId;
    private String courier;
    private String status;
}