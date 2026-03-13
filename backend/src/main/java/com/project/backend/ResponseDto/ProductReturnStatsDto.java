package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.util.Map;

import com.project.backend.entity.ReturnReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReturnStatsDto {
	 private Long productId;
	    private String productName;
	    private Long returnCount;
	    private BigDecimal totalRefundAmount;
	
}
