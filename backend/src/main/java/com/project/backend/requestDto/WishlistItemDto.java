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

	private Long productId;
	private String productName;
	private String productSlug;
	private String productBrand;
	private String productImage;
	private Double productPrice;
	private Double productMrp;
	private Integer discountPercentage;

	private Long variantId;
	private String variantSku;
	private String size;
	private String color;
	private Double variantPrice;
	private Integer availableStock;

	private Boolean inStock;
	private Boolean isActive;
	private String note;
	private Integer priority;

	private Instant addedAt;

	@Builder.Default
	private Boolean priceChanged = false;
	private Double oldPrice;
	private Boolean backInStock = false;
}