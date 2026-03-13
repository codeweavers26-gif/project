package com.project.backend.ResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.ReturnStatus;

import lombok.Data;

@Data
public class ReturnTrackingDto {
    private String returnNumber;
    private ReturnStatus currentStatus;
    private LocalDateTime estimatedCompletionDate;
    private List<TrackingStep> steps;
    
    @Data
    public static class TrackingStep {
        private String title;
        private String description;
        private LocalDateTime date;
        private boolean completed;
    }
}