package com.project.backend.requestDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to create an option for an attribute (e.g. Size = M, Color = Red)")
public class AttributeOptionRequestDto {

    @NotBlank
    @Schema(example = "Red", description = "Option display value")
    private String value;

    @NotNull
    @Schema(example = "1", description = "Sort order for UI display")
    private Integer sortOrder;

    @Schema(example = "true", description = "Whether this option is active")
    private Boolean active = true;
}
