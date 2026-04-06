package com.project.backend.requestDto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CancelItemsRequest {

    @NotEmpty(message = "Item IDs cannot be empty")
    private List<Long> orderItemIds;
}