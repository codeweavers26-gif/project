package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminSectionResponseDto {
    private Long id;
    private String name;
    private String imageUrl;
    private Boolean isActive;
}

