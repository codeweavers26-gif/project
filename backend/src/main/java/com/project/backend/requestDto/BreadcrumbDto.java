package com.project.backend.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BreadcrumbDto {
    private String level;
    private String name;
    private String link;
}