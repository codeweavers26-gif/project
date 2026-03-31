package com.project.backend.requestDto;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private Long sectionId;
    private Boolean isActive;
}