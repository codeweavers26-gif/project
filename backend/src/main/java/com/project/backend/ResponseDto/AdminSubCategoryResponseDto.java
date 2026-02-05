package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminSubCategoryResponseDto {
    private Long id;
    private String name;
    private Boolean isActive;
}
