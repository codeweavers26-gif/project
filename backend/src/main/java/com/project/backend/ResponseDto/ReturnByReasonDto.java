package com.project.backend.ResponseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnByReasonDto {
    private String reason;
    private Long count;
}
