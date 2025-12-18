package com.project.backend.requestDto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CheckoutRequestDto {
	 private Long locationId;
    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CartItemDto> items;
}
