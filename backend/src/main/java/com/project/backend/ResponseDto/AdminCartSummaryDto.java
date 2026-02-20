package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AdminCartSummaryDto {
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer totalItems;
    private Integer totalQuantity;
    private Double totalValue;
    private Instant lastActivity;
    private List<AdminCartItemDto> items;
}