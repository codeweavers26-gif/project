package com.project.backend.requestDto;

import java.util.List;

import com.project.backend.entity.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequestDto {

	 @NotNull(message = "Address ID is required")
	    private Long addressId;
	    
	    @NotNull(message = "Payment method is required")
	    private PaymentMethod paymentMethod;  // Add this field
	    
	    @NotEmpty(message = "Cart cannot be empty")
	    @Valid
	    private List<CartItemDto> items;
	}