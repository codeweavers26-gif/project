package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCartResponseDto {
    private Long cartId;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer totalItems;
    private Double totalPrice;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AdminCartItemDto> items;
}
