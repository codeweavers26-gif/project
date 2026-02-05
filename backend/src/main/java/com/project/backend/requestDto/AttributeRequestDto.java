package com.project.backend.requestDto;

import lombok.Data;

@Data
public class AttributeRequestDto {
    private String name;
    private Boolean filterable;
    private Boolean required;
    private Long categoryId; 
}
