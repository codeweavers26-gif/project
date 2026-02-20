package com.project.backend.ResponseDto;

import java.time.Instant;

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
	private String stockStatus;  // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private Integer reorderLevel; // Suggested reorder point
    private Instant lastUpdated;
    private Integer reservedStock; // Stock reserved for pending orders
    private Integer availableStock; // stock - reserved
}
