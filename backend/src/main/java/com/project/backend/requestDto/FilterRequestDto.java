package com.project.backend.requestDto;

import lombok.Data;
import java.util.List;

@Data
public class FilterRequestDto {
    // Hierarchy filters
    private Long sectionId;
    private Long categoryId;
    private Long subCategoryId;
    
    // Price filters
    private Double minPrice;
    private Double maxPrice;
    
    // Attribute filters
    private List<String> brands;
    private List<String> sizes;      // S, M, L, XL
    private List<String> colors;     // Red, Blue, Black
    private List<String> materials;  // Cotton, Polyester
    
    // Other filters
    private Boolean inStockOnly;
    private Double minRating;
    private Boolean codAvailable;
    
    // Sorting
    private String sortBy; // price_asc, price_desc, rating_desc, newest
    private Integer page = 0;
    private Integer size = 20;
}