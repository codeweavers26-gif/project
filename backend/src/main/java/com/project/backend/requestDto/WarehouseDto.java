package com.project.backend.requestDto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WarehouseDto {

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Request {
		@NotBlank
		private String name;

		private String code;

		@NotNull
		private Long locationId;

		private String address;
		private String city;
		private String state;
		private String pincode;

		private String contactPerson;
		private String contactPhone;
		private String contactEmail;

		private Boolean isDefault = false;
		private Boolean isActive = true;

		private Double latitude;
		private Double longitude;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Response {
		private Long id;
		private String name;
		private String code;
		private Long locationId;
		private String address;
		private String city;
		private String state;
		private String pincode;
		private String contactPerson;
		private String contactPhone;
		private String contactEmail;
		private Boolean isDefault;
		private Boolean isActive;
		private Double latitude;
		private Double longitude;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;

		// Inventory summary
		private Integer totalVariants;
		private Integer totalStock;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InventorySummary {
		private Long warehouseId;
		private String warehouseName;
		private Integer totalVariants;
		private Integer totalStock;
		private Integer lowStockCount;
		private Integer outOfStockCount;
	}
}