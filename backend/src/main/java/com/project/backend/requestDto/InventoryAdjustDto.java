package com.project.backend.requestDto;

import lombok.Data;

@Data
public class InventoryAdjustDto {
	  private Long productId;
	    private Long locationId;
	    private int delta; // +5 or -3
}
