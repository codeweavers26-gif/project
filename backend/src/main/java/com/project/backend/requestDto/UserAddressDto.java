package com.project.backend.requestDto;

import lombok.Data;

@Data
public class UserAddressDto {

	private String addressLine1;
	private String addressLine2;
	private String city;
	private String state;
	private String postalCode;
	private String country;
	private String addressType;
	private boolean isDefault;
}
