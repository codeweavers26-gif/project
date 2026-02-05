package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttributeOptionResponseDto {
    private Long id;
    private String value;
    private Boolean active;
}
