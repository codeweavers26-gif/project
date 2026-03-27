package com.project.backend.ResponseDto;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceabilityResponse {

    private boolean serviceable;
    private List<CourierOption> couriers;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourierOption {
        private String courierName;
        private double rate;
        private int deliveryDays;
        private boolean codAvailable;
    }
}