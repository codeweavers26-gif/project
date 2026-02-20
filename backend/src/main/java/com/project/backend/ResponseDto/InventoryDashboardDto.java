package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryDashboardDto {
    private Long totalProducts;
    private Long totalLocations;
    private Long totalInventoryItems;
    private Long outOfStockCount;
    private Long inStockCount;
    private Double averageStockPerProduct;
    private Double totalInventoryValue;
    private Integer lowStockThreshold;
    private Long lowStockCount;
    private List<LowStockItemDto> lowStockItems;
}

