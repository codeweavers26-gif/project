package com.project.backend.ResponseDto;

import java.time.Instant;

import com.project.backend.entity.ReturnReason;
import com.project.backend.entity.ReturnStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnResponseDto {
	private Long returnId;

	private Long orderId;

	private Long orderItemId;

	private Long productId;

	private String productName;

	private Integer quantity;

	private ReturnStatus status;

	private String reason;

	private Double refundAmount;

	private Instant requestedAt;
}
