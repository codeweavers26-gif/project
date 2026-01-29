package com.project.backend.requestDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Request payload for creating or updating a product")
public class ProductRequestDto {

    @Schema(description = "Product name", example = "Nike Running Shoes")
    private String name;

    @Schema(description = "Brand name", example = "Nike")
    private String brand;

    @Schema(description = "Unique product SKU", example = "NIKE-RUN-001")
    private String sku;

    @Schema(description = "SEO friendly product slug", example = "nike-running-shoes")
    private String slug;

    @JsonProperty("short_description")
    @Schema(description = "Short summary about the product", example = "Lightweight running shoes for men")
    private String shortDescription;

    @Schema(description = "Detailed product description", example = "Breathable mesh upper with durable rubber sole.")
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

    @Schema(description = "Product weight in kilograms", example = "0.8")
    private Double weight;

    @Schema(description = "Product length in cm", example = "30")
    private Double length;

    @Schema(description = "Product width in cm", example = "12")
    private Double width;

    @Schema(description = "Product height in cm", example = "10")
    private Double height;

    @JsonProperty("cod_available")
    @Schema(description = "Indicates if Cash on Delivery is available", example = "true")
    private Boolean codAvailable;

    @Schema(description = "Indicates if the product can be returned", example = "true")
    private Boolean returnable;

    @JsonProperty("delivery_days")
    @Schema(description = "Estimated delivery time in days", example = "5")
    private Integer deliveryDays;

    @Schema(description = "List of product image URLs (maximum 6 allowed)")
    private List<String> images;

//    @Schema(description = "Product attributes like color, size, material")
//    private Map<String, String> attributes;
    
    @Schema(description = "Category ID to which product belongs", example = "2")
    private Long categoryId;

}
