package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubCategoryAttributeResponseDto {
    private Long id;
    private Long subCategoryId;
    private Long attributeId;
}
