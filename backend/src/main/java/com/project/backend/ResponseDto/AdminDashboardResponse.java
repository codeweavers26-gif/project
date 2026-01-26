package com.project.backend.ResponseDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Admin dashboard metrics overview")
public class AdminDashboardResponse {

    @JsonProperty("total_sales")
    @Schema(description = "Total completed sales amount", example = "2489000.50")
    private Double totalSales;

    @JsonProperty("total_orders")
    @Schema(description = "Total number of delivered orders", example = "1240")
    private Long totalOrders;

    @JsonProperty("total_items_sold")
    @Schema(description = "Total items sold across all orders", example = "6821")
    private Long totalItemsSold;

    @JsonProperty("total_inventory_left")
    @Schema(description = "Total stock remaining in inventory", example = "15432")
    private Long totalInventoryLeft;

    @JsonProperty("total_users")
    @Schema(description = "Total registered customers", example = "5421")
    private Long totalUsers;

    @JsonProperty("users_with_cart")
    @Schema(description = "Number of users who currently have items in cart", example = "182")
    private Long usersWithCart;

    @JsonProperty("total_cart_value")
    @Schema(description = "Total value of all items currently in carts", example = "352190.50")
    private Double totalCartValue;
}
