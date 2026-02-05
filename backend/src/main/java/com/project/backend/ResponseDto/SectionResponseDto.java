package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SectionResponseDto {
    private Long id;
    private String name;
    private String imageUrl;
}
