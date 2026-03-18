package com.project.backend.requestDto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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

 private List<VariantRequest> variants;
    
    private List<ImageRequest> images;

    @Schema(description = "Category ID to which product belongs", example = "2")
    private Long categoryId;
    
    @Data
    public static class VariantRequest {
        @NotBlank
        private String size;
        
        @NotBlank
        private String color;
        
        @NotNull
        private BigDecimal mrp;
        @Schema(description = "Product price", example = "3499")
        @NotNull(message = "Price is required")
        private Double price;
        @NotNull
        private BigDecimal sellingPrice;
        
        private BigDecimal costPrice;
        
        @NotNull
        private Integer initialStock; 
    }
    
    @Data
    public static class ImageRequest {
        private String imageUrl;
        private Boolean isPrimary;
        private Integer displayOrder;
    }
    
    @Data
    public static class StockUpdateRequest {
        @NotNull(message = "Variant ID is required")
        private Long variantId;
        
        @NotNull(message = "Warehouse ID is required")
        private Long warehouseId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }

}
