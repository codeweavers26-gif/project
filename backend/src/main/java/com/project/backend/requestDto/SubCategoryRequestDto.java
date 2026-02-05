package com.project.backend.requestDto;

import lombok.Data;

@Data
public class SubCategoryRequestDto {
    private Long categoryId;
    private String name;
    private Boolean active; 
}
