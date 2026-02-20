package com.project.backend.requestDto;

import lombok.Data;
import java.time.Instant;

@Data
public class AdminCartFilterDto {
    private Long userId;
    private String userEmail;
    private String userName;
    private Long productId;
    private Integer minItems;
    private Integer maxItems;
    private Double minTotal;
    private Double maxTotal;
    private Instant fromDate;
    private Instant toDate;
    private Boolean abandonedOnly; // Carts older than 24 hours with items
}