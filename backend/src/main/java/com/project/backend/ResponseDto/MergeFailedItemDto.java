package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeFailedItemDto {
    private Long productId;
    private Long variantId;
    private String reason;
}