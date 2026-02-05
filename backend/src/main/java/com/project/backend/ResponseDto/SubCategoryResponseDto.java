package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubCategoryResponseDto {
    private Long id;
    private String name;
    private Long categoryId;
}
