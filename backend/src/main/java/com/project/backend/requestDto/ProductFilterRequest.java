package com.project.backend.requestDto;

import java.util.List;

import lombok.Data;

@Data
public class ProductFilterRequest {
    private Long categoryId;
    private List<String> sizes;
    private List<String> colors;
    private Double minPrice;
    private Double maxPrice;
    private String sortBy;
    private Integer page = 0;
    private Integer size = 20;
}