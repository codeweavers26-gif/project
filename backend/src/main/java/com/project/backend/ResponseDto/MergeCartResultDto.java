package com.project.backend.ResponseDto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartResultDto {
    private boolean success;
    private String message;
    private int mergedCount;
    private int failedCount;
    private List<MergeFailedItemDto> failedItems;
}