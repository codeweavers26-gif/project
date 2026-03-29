package com.project.backend.ResponseDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReturnItemDto {

    private Long orderItemId;
    private Integer quantity;
}