package com.project.backend.requestDto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductFilterDto {
	 private String search;
    private Long sectionId;
    private Long categoryId;
    private Long subCategoryId;
    private Double minPrice;
    private Double maxPrice;
    private String color;
    private String brand;
    private String size;
    private List<String> brands;
    private Boolean inStockOnly;
    private Double minRating;
    private Boolean codAvailable;
    private String sortBy;
    private Integer page;
    private Integer limit ;
}