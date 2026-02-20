package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CartStatisticsDto {
    private Long totalUsersWithCart;
    private Long totalCartItems;
    private Double totalCartValue;
    private Double averageCartValue;
    private Long abandonedCartsCount;
    private Double abandonedCartsValue;
    private Map<String, Long> hourlyActivity;
    private List<Map<String, Object>> topProducts;
}