package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartPricingResponseDto {

	private List<CartItemResponseDto> items;

	private Double subtotal; // sum of item prices
	private Double taxAmount; // total tax
	private Double shippingCharges; // delivery charge
	private Double discountAmount; // coupon or offer discount
	private Double finalAmount; // grand total

	private String appliedCoupon; // coupon code if applied
	private Boolean couponApplied; // success or not
	private String message; // error or success message
}
