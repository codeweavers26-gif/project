package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryResponseDto {

	private Long inventoryId;

	private Long productId;
	private String productName;

	private Long locationId;
	private String locationName;

	private Integer stock;
}
