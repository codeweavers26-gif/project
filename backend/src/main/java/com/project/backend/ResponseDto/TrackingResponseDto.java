package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TrackingResponseDto {
    private Long orderId;
    private String status;
    private String trackingId;
    private List<TrackingEvent> events;

    @Data
    @Builder
    public static class TrackingEvent {
        private String status;
        private String date;
        private String location;
    }
}