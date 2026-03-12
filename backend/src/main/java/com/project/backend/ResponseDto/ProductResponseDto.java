package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Detailed product information returned to client")
public class ProductResponseDto {

	private String status;
	@Schema(description = "Unique product ID", example = "101")
	private Long id;

	@Schema(description = "Product name", example = "Nike Running Shoes")
	private String name;

	@Schema(description = "Brand name", example = "Nike")
	private String brand;

	@Schema(description = "Product SKU", example = "NIKE-RUN-001")
	private String sku;

	@Schema(description = "SEO friendly product URL slug", example = "nike-running-shoes")
	private String slug;

	@JsonProperty("short_description")
	@Schema(description = "Short summary about the product")
	private String shortDescription;

	@Schema(description = "Full product description")
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

	@JsonProperty("in_stock")
	@Schema(description = "Indicates if product is currently in stock", example = "true")
	private Boolean inStock;

	@JsonProperty("stock_status")
	@Schema(description = "Stock status label", example = "IN_STOCK / LOW_STOCK / OUT_OF_STOCK")
	private String stockStatus;

	@JsonProperty("average_rating")
	private Double averageRating;

	@JsonProperty("total_reviews")
	private Integer totalReviews;

	@JsonProperty("cod_available")
	private Boolean codAvailable;

	private Boolean returnable;

	@JsonProperty("delivery_days")
	private Integer deliveryDays;

	private Double weight;
	private Double length;
	private Double width;
	private Double height;

	@JsonProperty("main_image")
	private String mainImage;

	@JsonProperty("thumbnail_image")
	private String thumbnailImage;

	@JsonProperty("medium_image")
	private String mediumImage;

	@Schema(description = "Product attributes like color, size etc.")
	private Map<String, String> attributes;

	@JsonProperty("is_active")
	private Boolean isActive;

	@JsonProperty("created_at")
	private Instant createdAt;
	private CategoryInfo category;
	private List<VariantInfo> variants;
	private List<ImageInfo> images;

	@Data
	@Builder
	public static class CategoryInfo {
		private Long id;
		private String name;
		private String slug;
	}

	@Data
	@Builder
	public static class VariantInfo {
		private Long id;
		private String sku;
		private String size;
		private String color;
		private BigDecimal mrp;
		 private BigDecimal costPrice;
		private BigDecimal sellingPrice;
		private Boolean isActive;
		private Integer availableStock; // From warehouse_inventory
	}

	@Data
	@Builder
	public static class ImageInfo {
		private Long id;
		private String imageUrl;
		private Boolean isPrimary;
		private Integer position;
	}
}
