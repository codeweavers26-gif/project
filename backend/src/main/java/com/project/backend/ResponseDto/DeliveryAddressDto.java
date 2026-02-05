package com.project.backend.ResponseDto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryAddressDto {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
