package com.project.backend.requestDto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistItemDto {
	private Long id;

	// Product Info
	private Long productId;
	private String productName;
	private String productSlug;
	private String productBrand;
	private String productImage;
	private Double productPrice;
	private Double productMrp;
	private Integer discountPercentage;

	// Variant Info (if applicable)
	private Long variantId;
	private String variantSku;
	private String size;
	private String color;
	private Double variantPrice;
	private Integer availableStock;

	// Item Status
	private Boolean inStock;
	private Boolean isActive;
	private String note;
	private Integer priority;

	// Timestamps
	private Instant addedAt;

	// Computed fields
	@Builder.Default
	private Boolean priceChanged = false;
	private Double oldPrice;
	private Boolean backInStock = false;
}