package com.project.backend.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequestDto {

    @NotNull
    private Integer delta; 
}
