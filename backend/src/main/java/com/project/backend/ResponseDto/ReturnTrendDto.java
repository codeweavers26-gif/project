package com.project.backend.ResponseDto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnTrendDto {

    private LocalDate date;
    private Long count;
}
