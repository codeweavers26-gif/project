package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.backend.requestDto.WishlistItemDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private Integer totalItems;
    private List<WishlistItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
    
    private Long totalWishlists; 
    private Double averageItemsPerWishlist; 
}
