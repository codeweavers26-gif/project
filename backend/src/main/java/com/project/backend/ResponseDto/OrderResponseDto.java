package com.project.backend.ResponseDto;

import java.time.Instant;
import java.util.List;

import com.project.backend.entity.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDto {

	private Long orderId;
	private Double totalAmount;
	private OrderStatus status;
	private Instant createdAt;
	private List<OrderItemResponseDto> items;
}
