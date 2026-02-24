package com.project.backend.requestDto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantAvailabilityDto {
    private Long variantId;
    private String size;
    private String color;
    private String sku;
    private Boolean inStock;
    private Integer availableQuantity;
    private BigDecimal sellingPrice;
}