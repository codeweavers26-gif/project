package com.project.backend.ResponseDto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public  class TrackingStep {
    private String title;
    private String description;
    private LocalDateTime date;
    private boolean completed;
    private boolean active;
}