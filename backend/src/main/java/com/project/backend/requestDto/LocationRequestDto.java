
package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationRequestDto {

	@NotBlank(message = "City is required")
	private String city;

	private String state;

	@NotBlank(message = "Pincode is required")
	private String pincode;
}
