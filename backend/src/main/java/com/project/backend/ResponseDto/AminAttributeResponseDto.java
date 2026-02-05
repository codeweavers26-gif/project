package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AminAttributeResponseDto {
    private Long id;
    private String name;
    private Boolean filterable;
}
