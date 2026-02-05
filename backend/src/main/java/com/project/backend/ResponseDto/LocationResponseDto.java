package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationResponseDto {
    private Long id;
    private String name;
    private String city;
    private String state;
    private String pincode;
    private Integer deliveryDays;
    private Boolean codAvailable;
}
