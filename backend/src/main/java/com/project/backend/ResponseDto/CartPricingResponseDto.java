package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartPricingResponseDto {

	 private List<CartItemResponseDto> items;
	    private BigDecimal subtotal;
	    private BigDecimal taxAmount;
	    private BigDecimal shippingCharges;
	    private BigDecimal discountAmount;
	    private BigDecimal finalAmount;
	    private String appliedCoupon;
	    private Boolean couponApplied;
	    private String message;
	    private Integer totalItems;
	    private BigDecimal totalMrp;
 private BigDecimal totalSavings;
}
