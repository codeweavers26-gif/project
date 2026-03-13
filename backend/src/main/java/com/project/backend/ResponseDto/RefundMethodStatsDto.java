package com.project.backend.ResponseDto;

import java.math.BigDecimal;

import com.project.backend.entity.RefundMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundMethodStatsDto {
    private RefundMethod method;
    private Long count;
    private BigDecimal totalAmount;
    private Double percentage;
    }