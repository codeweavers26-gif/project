package com.project.backend.requestDto;

import lombok.Data;

@Data
public class CategoryRequestDto {
    private String name;
    private Long sectionId;
    private Boolean active;
}
