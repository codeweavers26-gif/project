package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String pincode;

    private Double latitude;
    private Double longitude;

    @NotNull
    private Integer deliveryDays;

    @NotNull
    private Boolean codAvailable;

    private Double extraShippingCharge;
}
