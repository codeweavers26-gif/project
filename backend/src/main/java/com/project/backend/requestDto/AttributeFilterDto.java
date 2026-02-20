package com.project.backend.requestDto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AttributeFilterDto {
    private String name;
    private String displayName;
    private List<String> options;
}