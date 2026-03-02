package com.project.backend.requestDto;

import com.project.backend.entity.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequestDto {

	 @NotNull(message = "Address ID is required")
	    private Long addressId;
	    
	    @NotNull(message = "Payment method is required")
	    private PaymentMethod paymentMethod;  
	    
	    @Valid
	    private Integer cartId;
	}