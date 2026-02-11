package com.project.backend.ResponseDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReorderResponseDto {

    private boolean success;
    private String message;

    private List<ReorderItemSuccessDto> addedItems;
    private List<ReorderItemFailureDto> failedItems;
}
