package com.project.backend.requestDto;

import lombok.Data;
import java.util.List;

@Data
public class ProductFilterDto {
    private Long sectionId;
    private Long categoryId;
    private Long subCategoryId;
    private Double minPrice;
    private Double maxPrice;
    private List<String> brands;
    private Boolean inStockOnly;
    private Double minRating;
    private Boolean codAvailable;
    private String sortBy;
    private Integer page = 0;
    private Integer size = 20;
}