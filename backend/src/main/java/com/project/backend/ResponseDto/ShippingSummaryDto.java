package com.project.backend.ResponseDto;

import lombok.Data;

@Data
public class ShippingSummaryDto {
    private Long readyToShip;
    private Long inTransit;
    private Long deliveredToday;
}

