package com.project.backend.requestDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryRequestDto {

    @NotNull
    private Long productId;

    @NotNull
    private Long locationId;

    @Min(0)
    private Integer stock;
}
