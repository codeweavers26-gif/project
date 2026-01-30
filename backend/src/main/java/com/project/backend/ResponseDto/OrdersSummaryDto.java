package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrdersSummaryDto {
    private Long today;
    private Long thisWeek;
    private Long thisMonth;
}
