package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartPricingResponseDto {

	 private List<CartItemResponseDto> items;
	    private Double subtotal;
	    private Double taxAmount;
	    private Double shippingCharges;
	    private Double discountAmount;
	    private Double finalAmount;
	    private String appliedCoupon;
	    private Boolean couponApplied;
	    private String message;
}
