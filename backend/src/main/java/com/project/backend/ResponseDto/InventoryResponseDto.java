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
	private String stockStatus;  
    private Integer reorderLevel;
    private Instant lastUpdated;
    private Integer reservedStock; 
    private Integer availableStock; 
}
