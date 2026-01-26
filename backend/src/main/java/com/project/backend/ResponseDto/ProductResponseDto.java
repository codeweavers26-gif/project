package com.project.backend.ResponseDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Detailed product information returned to client")
public class ProductResponseDto {

    @Schema(description = "Unique product ID", example = "101")
    private Long id;

    @Schema(description = "Product name", example = "Nike Running Shoes")
    private String name;

    @Schema(description = "Brand name", example = "Nike")
    private String brand;

    @Schema(description = "SEO friendly product URL slug", example = "nike-running-shoes")
    private String slug;

    @JsonProperty("short_description")
    @Schema(description = "Short summary about the product", example = "Lightweight running shoes for men")
    private String shortDescription;

    @Schema(description = "Full product description", example = "Breathable mesh upper with durable rubber sole.")
    private String description;

    @Schema(description = "Maximum retail price", example = "4999")
    private Double mrp;

    @Schema(description = "Selling price", example = "3499")
    private Double price;

    @JsonProperty("discount_percent")
    @Schema(description = "Discount percentage applied", example = "30")
    private Double discountPercent;

    @JsonProperty("tax_percent")
    @Schema(description = "Tax percentage applied", example = "12")
    private Double taxPercent;

    @Schema(description = "Available stock quantity", example = "120")
    private Integer stock;

    @JsonProperty("average_rating")
    @Schema(description = "Average customer rating", example = "4.3")
    private Double averageRating;

    @JsonProperty("total_reviews")
    @Schema(description = "Total number of reviews", example = "245")
    private Integer totalReviews;

    @JsonProperty("cod_available")
    @Schema(description = "Indicates if Cash on Delivery is available", example = "true")
    private Boolean codAvailable;

    @Schema(description = "Indicates if product is returnable", example = "true")
    private Boolean returnable;

    @JsonProperty("delivery_days")
    @Schema(description = "Estimated delivery time in days", example = "5")
    private Integer deliveryDays;

    @Schema(description = "List of product image URLs")
    private List<String> images;

    @Schema(description = "Product attributes like color, size etc.")
    private Map<String, String> attributes;

    @JsonProperty("is_active")
    @Schema(description = "Indicates if product is active and visible to users", example = "true")
    private Boolean isActive;
}
