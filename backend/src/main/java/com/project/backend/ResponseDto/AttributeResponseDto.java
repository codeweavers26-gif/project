package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttributeResponseDto {
    private String name;
    private Boolean required;
    private String inputType;
    private List<String> options;
}

