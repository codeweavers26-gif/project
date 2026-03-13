package com.project.backend.requestDto;

import com.project.backend.entity.ReturnReason;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Return reason is required")
    private ReturnReason reason;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String reasonDescription;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String comments;
}